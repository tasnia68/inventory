package com.inventory.system.service.impl;

import com.inventory.system.common.entity.ReturnMerchandiseAuthorization;
import com.inventory.system.common.entity.ReturnMerchandiseItem;
import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.DeliveryReviewStatus;
import com.inventory.system.common.entity.PickingStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShipmentItem;
import com.inventory.system.common.entity.ShipmentQueueType;
import com.inventory.system.common.entity.ShipmentStatus;
import com.inventory.system.common.entity.ShipmentTimelineEvent;
import com.inventory.system.common.entity.ShipmentTimelineEventType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateRmaItemRequest;
import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.CreateShipmentItemRequest;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.DeliveryConfirmationRequest;
import com.inventory.system.payload.DeliveryNoteDto;
import com.inventory.system.payload.GenerateShippingLabelRequest;
import com.inventory.system.payload.RmaDto;
import com.inventory.system.payload.RmaItemDto;
import com.inventory.system.payload.ShipmentDeliveryReviewActionRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentQueueSummaryDto;
import com.inventory.system.payload.ShipmentItemDto;
import com.inventory.system.payload.ShipmentSearchRequest;
import com.inventory.system.payload.ShipmentTimelineEventDto;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.payload.UpdateShipmentTrackingRequest;
import com.inventory.system.repository.ReturnMerchandiseAuthorizationRepository;
import com.inventory.system.repository.ReturnMerchandiseItemRepository;
import com.inventory.system.repository.PickingTaskRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.repository.ShipmentItemRepository;
import com.inventory.system.service.ShipmentService;
import com.inventory.system.service.TenantSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ReturnMerchandiseAuthorizationRepository rmaRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ReturnMerchandiseItemRepository returnMerchandiseItemRepository;
    private final TenantSettingService tenantSettingService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    public ShipmentDto createShipment(CreateShipmentRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order", "id", request.getSalesOrderId()));

        if (salesOrder.getStatus() != SalesOrderStatus.CONFIRMED
            && salesOrder.getStatus() != SalesOrderStatus.BACKORDERED
            && salesOrder.getStatus() != SalesOrderStatus.PARTIALLY_SHIPPED) {
            throw new BadRequestException("Shipment can only be created for CONFIRMED, BACKORDERED or PARTIALLY_SHIPPED sales orders");
        }

        Map<UUID, SalesOrderItem> orderItemsById = new HashMap<>();
        for (SalesOrderItem orderItem : salesOrder.getItems()) {
            orderItemsById.put(orderItem.getId(), orderItem);
        }

        Shipment shipment = new Shipment();
        String courierProvider = trimToNull(request.getCourierProvider());
        if (courierProvider == null) {
            courierProvider = tenantSettingService.findSetting("sales.fulfillment.defaultCourierProvider")
                .map(setting -> trimToNull(setting.getValue()))
                .orElse(null);
        }

        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setSalesOrder(salesOrder);
        shipment.setWarehouse(salesOrder.getWarehouse());
        shipment.setCarrier(request.getCarrier());
        shipment.setCourierProvider(courierProvider);
        shipment.setCourierService(trimToNull(request.getCourierService()));
        shipment.setCourierReference(trimToNull(request.getCourierReference()));
        shipment.setCashOnDeliveryAmount(request.getCashOnDeliveryAmount());
        shipment.setDeliveryFee(request.getDeliveryFee());
        shipment.setNotes(request.getNotes());
        shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
        shipment.setCourierDispatchStatus(
                shipment.getCourierProvider() != null ? CourierDispatchStatus.BOOKED : CourierDispatchStatus.UNASSIGNED
        );
        appendTimelineEvent(
            shipment,
            ShipmentTimelineEventType.SHIPMENT_CREATED,
            "system",
            "Shipment created",
            "Shipment entered the fulfillment flow and is ready for courier handling.",
            LocalDateTime.now(),
            true
        );

        List<ShipmentItem> shipmentItems = new ArrayList<>();

        for (CreateShipmentItemRequest itemRequest : request.getItems()) {
            SalesOrderItem salesOrderItem = orderItemsById.get(itemRequest.getSalesOrderItemId());
            if (salesOrderItem == null) {
                throw new BadRequestException("Sales order item " + itemRequest.getSalesOrderItemId() + " does not belong to this sales order");
            }

            BigDecimal remaining = salesOrderItem.getQuantity().subtract(salesOrderItem.getShippedQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Sales order item " + salesOrderItem.getId() + " is already fully shipped");
            }
            if (itemRequest.getQuantity().compareTo(remaining) > 0) {
                throw new BadRequestException("Shipment quantity exceeds remaining quantity for sales order item " + salesOrderItem.getId());
            }

            BigDecimal completedPickedQuantity = pickingTaskRepository.sumPickedQuantityBySalesOrderItemIdForCompletedLists(
                    salesOrderItem.getId(),
                    PickingStatus.COMPLETED
            );
            BigDecimal shippableQuantity = completedPickedQuantity.subtract(salesOrderItem.getShippedQuantity());
            if (itemRequest.getQuantity().compareTo(shippableQuantity) > 0) {
                throw new BadRequestException(
                        "Shipment quantity exceeds completed picked quantity for sales order item "
                                + salesOrderItem.getId()
                                + ". Picked and not yet shipped: "
                                + shippableQuantity
                );
            }

            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setShipment(shipment);
            shipmentItem.setSalesOrderItem(salesOrderItem);
            shipmentItem.setProductVariant(salesOrderItem.getProductVariant());
            shipmentItem.setQuantity(itemRequest.getQuantity());
            shipmentItems.add(shipmentItem);

            salesOrderItem.setShippedQuantity(salesOrderItem.getShippedQuantity().add(itemRequest.getQuantity()));
        }

        shipment.setItems(shipmentItems);

        updateSalesOrderShipmentStatus(salesOrder);
        Shipment saved = shipmentRepository.save(shipment);
        salesOrderRepository.save(salesOrder);

        return mapShipmentToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDto getShipment(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));
        return mapShipmentToDto(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getAllShipments(ShipmentSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<Shipment> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getSalesOrderId() != null) {
                predicates.add(cb.equal(root.get("salesOrder").get("id"), request.getSalesOrderId()));
            }
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getShipmentNumber() != null && !request.getShipmentNumber().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("shipmentNumber")), "%" + request.getShipmentNumber().toUpperCase() + "%"));
            }
            if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("trackingNumber")), "%" + request.getTrackingNumber().toUpperCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return shipmentRepository.findAll(spec, pageable).map(this::mapShipmentToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByQueue(ShipmentQueueType queue, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return shipmentRepository.findAll(buildShipmentQueueSpecification(queue), pageable).map(this::mapShipmentToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentQueueSummaryDto getShipmentQueueSummary() {
        ShipmentQueueSummaryDto summary = new ShipmentQueueSummaryDto();
        summary.setReadyToHandoffCount(shipmentRepository.count(buildShipmentQueueSpecification(ShipmentQueueType.READY_TO_HANDOFF)));
        summary.setInTransitCount(shipmentRepository.count(buildShipmentQueueSpecification(ShipmentQueueType.IN_TRANSIT)));
        summary.setNeedsActionCount(shipmentRepository.count(buildShipmentQueueSpecification(ShipmentQueueType.NEEDS_ACTION)));
        summary.setDeliveryReviewPendingCount(shipmentRepository.count((root, query, cb) -> cb.equal(root.get("deliveryReviewStatus"), DeliveryReviewStatus.PENDING)));
        summary.setDeliveryReviewDisputedCount(shipmentRepository.count((root, query, cb) -> cb.equal(root.get("deliveryReviewStatus"), DeliveryReviewStatus.DISPUTED)));
        return summary;
    }

    @Override
    public ShipmentDto updateTracking(UUID shipmentId, UpdateShipmentTrackingRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new BadRequestException("Tracking cannot be updated for closed shipments");
        }

        ShipmentStatus previousStatus = shipment.getStatus();
        CourierDispatchStatus previousDispatchStatus = shipment.getCourierDispatchStatus();
        DeliveryReviewStatus previousReviewStatus = shipment.getDeliveryReviewStatus();
        String previousTrackingNumber = shipment.getTrackingNumber();
        String previousCourierReference = shipment.getCourierReference();

        if (request.getCarrier() != null) {
            shipment.setCarrier(trimToNull(request.getCarrier()));
        }
        if (request.getCourierProvider() != null) {
            shipment.setCourierProvider(trimToNull(request.getCourierProvider()));
        }
        if (request.getCourierService() != null) {
            shipment.setCourierService(trimToNull(request.getCourierService()));
        }
        if (request.getCourierReference() != null) {
            shipment.setCourierReference(trimToNull(request.getCourierReference()));
        }
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            shipment.setTrackingNumber(request.getTrackingNumber().trim());
            if (request.getTrackingUrl() == null || request.getTrackingUrl().isBlank()) {
                shipment.setTrackingUrl(buildTrackingUrl(shipment.getCarrier(), shipment.getTrackingNumber()));
            }
        }
        if (request.getTrackingUrl() != null && !request.getTrackingUrl().isBlank()) {
            shipment.setTrackingUrl(request.getTrackingUrl().trim());
        }
        if (request.getCashOnDeliveryAmount() != null) {
            shipment.setCashOnDeliveryAmount(request.getCashOnDeliveryAmount());
        }
        if (request.getDeliveryFee() != null) {
            shipment.setDeliveryFee(request.getDeliveryFee());
        }
        if (request.getLastCourierEvent() != null) {
            shipment.setLastCourierEvent(trimToNull(request.getLastCourierEvent()));
        }
        if (request.getLastCourierSyncAt() != null) {
            shipment.setLastCourierSyncAt(request.getLastCourierSyncAt());
        }
        applyDeliveryReviewUpdate(
                shipment,
                request.getDeliveryReviewStatus(),
                request.getDeliveryReviewReason()
        );
        if (request.getPickupRequestedAt() != null) {
            shipment.setPickupRequestedAt(request.getPickupRequestedAt());
        }
        if (request.getPickedUpAt() != null) {
            shipment.setPickedUpAt(request.getPickedUpAt());
        }
        if (request.getOutForDeliveryAt() != null) {
            shipment.setOutForDeliveryAt(request.getOutForDeliveryAt());
        }
        if (request.getCourierDispatchStatus() != null) {
            shipment.setCourierDispatchStatus(request.getCourierDispatchStatus());
        }
        if (request.getStatus() != null) {
            shipment.setStatus(request.getStatus());
        }

        applyCourierState(shipment);
    appendTimelineEventsForTrackingUpdate(
        shipment,
        previousStatus,
        previousDispatchStatus,
        previousReviewStatus,
        previousTrackingNumber,
        previousCourierReference,
        request
    );

        Shipment saved = shipmentRepository.save(shipment);
        synchronizeSalesOrderDeliveryStatus(saved.getSalesOrder());
        if (saved.getTrackingUrl() != null && !saved.getTrackingUrl().isBlank()) {
            eventPublisher.publishEvent(new com.inventory.system.service.order.events.ShipmentTrackingUpdatedEvent(
                    saved.getId(),
                    saved.getSalesOrder() != null ? saved.getSalesOrder().getId() : null,
                    java.time.Instant.now()));
        }
        return mapShipmentToDto(saved);
    }

    @Override
    public ShipmentDto generateShippingLabel(UUID shipmentId, GenerateShippingLabelRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new BadRequestException("Shipping labels cannot be generated for closed shipments");
        }

        String baseLabelUrl = "https://labels.inventory.local/shipments/" + shipment.getShipmentNumber() + ".pdf";
        if (request != null && request.getServiceLevel() != null && !request.getServiceLevel().isBlank()) {
            shipment.setShippingLabelUrl(baseLabelUrl + "?serviceLevel=" + request.getServiceLevel());
        } else {
            shipment.setShippingLabelUrl(baseLabelUrl);
        }

        Shipment saved = shipmentRepository.save(shipment);
        return mapShipmentToDto(saved);
    }

    @Override
    public ShipmentDto confirmDelivery(UUID shipmentId, DeliveryConfirmationRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new BadRequestException("Cancelled or returned shipments cannot be delivered");
        }
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new BadRequestException("Shipment has already been delivered");
        }

        DeliveryReviewStatus previousReviewStatus = shipment.getDeliveryReviewStatus();

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setCourierDispatchStatus(CourierDispatchStatus.DELIVERED);
        shipment.setDeliveredDate(request != null && request.getDeliveredAt() != null ? request.getDeliveredAt() : LocalDateTime.now());
        if (shipment.getShippedDate() == null) {
            shipment.setShippedDate(LocalDateTime.now());
        }
        shipment.setDeliveryReviewStatus(DeliveryReviewStatus.APPROVED);
        if (shipment.getDeliveryReviewRequestedAt() == null) {
            shipment.setDeliveryReviewRequestedAt(shipment.getDeliveredDate());
        }
        shipment.setDeliveryReviewResolvedAt(shipment.getDeliveredDate());

        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            shipment.setNotes(request.getNotes());
        }
        if (request != null && request.getProofOfDeliveryUrl() != null && !request.getProofOfDeliveryUrl().isBlank()) {
            shipment.setProofOfDeliveryUrl(request.getProofOfDeliveryUrl().trim());
        }
        if (request != null && request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            shipment.setProofOfDeliveryRecipientName(request.getRecipientName().trim());
        }
        if (shipment.getProofOfDeliveryUrl() != null || shipment.getProofOfDeliveryRecipientName() != null) {
            shipment.setProofOfDeliveryCapturedAt(shipment.getDeliveredDate());
            appendTimelineEvent(
                    shipment,
                    ShipmentTimelineEventType.PROOF_OF_DELIVERY_CAPTURED,
                    "manual",
                    "Proof of delivery recorded",
                    buildProofOfDeliveryDetails(shipment),
                    shipment.getProofOfDeliveryCapturedAt(),
                    false
            );
        }
                if (previousReviewStatus != DeliveryReviewStatus.APPROVED) {
                    appendTimelineEvent(
                        shipment,
                        ShipmentTimelineEventType.DELIVERY_REVIEW_RESOLVED,
                        "manual",
                        "Delivery review approved",
                        trimToNull(shipment.getDeliveryReviewReason()),
                        shipment.getDeliveredDate(),
                        false
                    );
                }
        appendTimelineEvent(
                shipment,
                ShipmentTimelineEventType.DELIVERY_CONFIRMED,
                "manual",
                "Delivery confirmed",
                request != null ? trimToNull(request.getNotes()) : null,
                shipment.getDeliveredDate(),
                true
        );

        Shipment savedShipment = shipmentRepository.save(shipment);

        synchronizeSalesOrderDeliveryStatus(savedShipment.getSalesOrder());

        return mapShipmentToDto(savedShipment);
    }

    @Override
    public ShipmentDto approveDelivery(UUID shipmentId, ShipmentDeliveryReviewActionRequest request) {
        return updateDeliveryReviewStatus(shipmentId, DeliveryReviewStatus.APPROVED, request);
    }

    @Override
    public ShipmentDto disputeDelivery(UUID shipmentId, ShipmentDeliveryReviewActionRequest request) {
        return updateDeliveryReviewStatus(shipmentId, DeliveryReviewStatus.DISPUTED, request);
    }

    @Override
    public DeliveryNoteDto generateDeliveryNote(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        String note = buildDeliveryNoteText(shipment);
        shipment.setDeliveryNote(note);
        shipmentRepository.save(shipment);

        DeliveryNoteDto dto = new DeliveryNoteDto();
        dto.setShipmentId(shipment.getId());
        dto.setShipmentNumber(shipment.getShipmentNumber());
        dto.setNote(note);
        dto.setGeneratedAt(LocalDateTime.now());
        return dto;
    }

    @Override
    public RmaDto createRma(CreateRmaRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order", "id", request.getSalesOrderId()));

        if (salesOrder.getStatus() != SalesOrderStatus.SHIPPED && salesOrder.getStatus() != SalesOrderStatus.DELIVERED && salesOrder.getStatus() != SalesOrderStatus.RETURNED) {
            throw new BadRequestException("RMA can only be created for shipped or delivered sales orders");
        }

        Shipment shipment = null;
        if (request.getShipmentId() != null) {
            shipment = shipmentRepository.findById(request.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", request.getShipmentId()));
            if (!shipment.getSalesOrder().getId().equals(salesOrder.getId())) {
                throw new BadRequestException("Shipment does not belong to the provided sales order");
            }
        }

        Map<UUID, SalesOrderItem> orderItemsById = new HashMap<>();
        for (SalesOrderItem orderItem : salesOrder.getItems()) {
            orderItemsById.put(orderItem.getId(), orderItem);
        }

        ReturnMerchandiseAuthorization rma = new ReturnMerchandiseAuthorization();
        rma.setRmaNumber(generateRmaNumber());
        rma.setSalesOrder(salesOrder);
        rma.setShipment(shipment);
        rma.setStatus(ReturnMerchandiseStatus.REQUESTED);
        rma.setReason(request.getReason());
        rma.setNotes(request.getNotes());
        rma.setRequestedAt(LocalDateTime.now());

        List<ReturnMerchandiseItem> items = new ArrayList<>();
        for (CreateRmaItemRequest itemRequest : request.getItems()) {
            SalesOrderItem salesOrderItem = orderItemsById.get(itemRequest.getSalesOrderItemId());
            if (salesOrderItem == null) {
                throw new BadRequestException("Sales order item " + itemRequest.getSalesOrderItemId() + " does not belong to this sales order");
            }

                BigDecimal alreadyRequestedForReturn = shipment != null
                    ? returnMerchandiseItemRepository.sumQuantityBySalesOrderItemIdAndShipmentIdAndRmaStatusIn(
                        salesOrderItem.getId(),
                        shipment.getId(),
                        EnumSet.of(
                            ReturnMerchandiseStatus.REQUESTED,
                            ReturnMerchandiseStatus.APPROVED,
                            ReturnMerchandiseStatus.RECEIVED,
                            ReturnMerchandiseStatus.COMPLETED
                        )
                    )
                    : returnMerchandiseItemRepository.sumQuantityBySalesOrderItemIdAndRmaStatusIn(
                        salesOrderItem.getId(),
                        EnumSet.of(
                            ReturnMerchandiseStatus.REQUESTED,
                            ReturnMerchandiseStatus.APPROVED,
                            ReturnMerchandiseStatus.RECEIVED,
                            ReturnMerchandiseStatus.COMPLETED
                        )
                    );

                BigDecimal maxReturnableQuantity = shipment != null
                    ? shipmentItemRepository.sumQuantityBySalesOrderItemIdAndShipmentId(salesOrderItem.getId(), shipment.getId())
                    : salesOrderItem.getShippedQuantity();

                BigDecimal remainingReturnable = maxReturnableQuantity.subtract(alreadyRequestedForReturn);
                if (itemRequest.getQuantity().compareTo(remainingReturnable) > 0) {
                throw new BadRequestException(
                    "RMA quantity cannot exceed remaining returnable quantity for sales order item "
                        + salesOrderItem.getId()
                        + ". Remaining: "
                        + remainingReturnable
                );
            }

            ReturnMerchandiseItem rmaItem = new ReturnMerchandiseItem();
            rmaItem.setRma(rma);
            rmaItem.setSalesOrderItem(salesOrderItem);
            rmaItem.setProductVariant(salesOrderItem.getProductVariant());
            rmaItem.setQuantity(itemRequest.getQuantity());
            rmaItem.setReason(itemRequest.getReason());
            items.add(rmaItem);
        }

        rma.setItems(items);
        ReturnMerchandiseAuthorization saved = rmaRepository.save(rma);
        return mapRmaToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RmaDto getRma(UUID id) {
        ReturnMerchandiseAuthorization rma = rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RMA", "id", id));
        return mapRmaToDto(rma);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RmaDto> getAllRmas(UUID salesOrderId, String rmaNumber, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<ReturnMerchandiseAuthorization> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (salesOrderId != null) {
                predicates.add(cb.equal(root.get("salesOrder").get("id"), salesOrderId));
            }
            if (rmaNumber != null && !rmaNumber.isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("rmaNumber")), "%" + rmaNumber.toUpperCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return rmaRepository.findAll(spec, pageable).map(this::mapRmaToDto);
    }

    @Override
    public RmaDto updateRmaStatus(UUID id, UpdateRmaStatusRequest request) {
        ReturnMerchandiseAuthorization rma = rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RMA", "id", id));

        validateRmaStatusTransition(rma.getStatus(), request.getStatus());

        rma.setStatus(request.getStatus());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            rma.setNotes(request.getNotes());
        }

        if (request.getStatus() == ReturnMerchandiseStatus.APPROVED) {
            rma.setApprovedAt(LocalDateTime.now());
        }
        if (request.getStatus() == ReturnMerchandiseStatus.RECEIVED) {
            rma.setReceivedAt(LocalDateTime.now());
        }
        if (request.getStatus() == ReturnMerchandiseStatus.COMPLETED) {
            rma.setCompletedAt(LocalDateTime.now());
            SalesOrder salesOrder = rma.getSalesOrder();
            boolean fullyReturned = salesOrder.getItems().stream().allMatch(item -> {
                BigDecimal completedReturns = returnMerchandiseItemRepository.sumQuantityBySalesOrderItemIdAndRmaStatusIn(
                        item.getId(),
                        EnumSet.of(ReturnMerchandiseStatus.COMPLETED)
                );
                return completedReturns.compareTo(item.getShippedQuantity()) >= 0;
            });
            if (fullyReturned) {
                salesOrder.setStatus(SalesOrderStatus.RETURNED);
            }
            salesOrderRepository.save(salesOrder);
        }

        ReturnMerchandiseAuthorization saved = rmaRepository.save(rma);
        return mapRmaToDto(saved);
    }

    private void updateSalesOrderShipmentStatus(SalesOrder salesOrder) {
        boolean anyShipped = salesOrder.getItems().stream().anyMatch(i -> i.getShippedQuantity().compareTo(BigDecimal.ZERO) > 0);
        boolean allShipped = salesOrder.getItems().stream().allMatch(i -> i.getShippedQuantity().compareTo(i.getQuantity()) >= 0);

        if (allShipped) {
            salesOrder.setStatus(SalesOrderStatus.SHIPPED);
            return;
        }
        if (anyShipped) {
            salesOrder.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
        }
    }

    private ShipmentDto updateDeliveryReviewStatus(UUID shipmentId, DeliveryReviewStatus targetStatus, ShipmentDeliveryReviewActionRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        validateDeliveryReviewAction(shipment, targetStatus, request);

        if (shipment.getDeliveryReviewStatus() == targetStatus) {
            return mapShipmentToDto(shipment);
        }

        String reason = request != null ? trimToNull(request.getReason()) : null;
        applyDeliveryReviewUpdate(shipment, targetStatus, reason);

        if (targetStatus == DeliveryReviewStatus.APPROVED
                && shipment.getCourierDispatchStatus() == CourierDispatchStatus.DELIVERED
                && shipment.getDeliveredDate() == null) {
            shipment.setDeliveredDate(LocalDateTime.now());
        }

        appendTimelineEvent(
                shipment,
                isOpenDeliveryReview(targetStatus)
                        ? ShipmentTimelineEventType.DELIVERY_REVIEW_REQUESTED
                        : ShipmentTimelineEventType.DELIVERY_REVIEW_RESOLVED,
                "manual",
                buildDeliveryReviewSummary(targetStatus),
                reason,
                LocalDateTime.now(),
                false
        );

        Shipment saved = shipmentRepository.save(shipment);
        synchronizeSalesOrderDeliveryStatus(saved.getSalesOrder());
        return mapShipmentToDto(saved);
    }

    private void validateDeliveryReviewAction(Shipment shipment, DeliveryReviewStatus targetStatus, ShipmentDeliveryReviewActionRequest request) {
        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new BadRequestException("Delivery review cannot be updated for closed shipments");
        }

        boolean hasDeliverySignal = shipment.getStatus() == ShipmentStatus.DELIVERED
                || shipment.getCourierDispatchStatus() == CourierDispatchStatus.DELIVERED
                || shipment.getDeliveredDate() != null
                || shipment.getProofOfDeliveryCapturedAt() != null;

        if (!hasDeliverySignal) {
            throw new BadRequestException("Delivery review can only be updated after a delivery signal is recorded");
        }

        if (targetStatus == DeliveryReviewStatus.DISPUTED) {
            String reason = request != null ? trimToNull(request.getReason()) : null;
            if (reason == null) {
                throw new BadRequestException("A dispute reason is required when marking a delivery as disputed");
            }
        }
    }

    private void validateRmaStatusTransition(ReturnMerchandiseStatus currentStatus, ReturnMerchandiseStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        switch (currentStatus) {
            case REQUESTED:
                if (targetStatus == ReturnMerchandiseStatus.APPROVED
                        || targetStatus == ReturnMerchandiseStatus.REJECTED
                        || targetStatus == ReturnMerchandiseStatus.CANCELLED) {
                    return;
                }
                break;
            case APPROVED:
                if (targetStatus == ReturnMerchandiseStatus.RECEIVED || targetStatus == ReturnMerchandiseStatus.CANCELLED) {
                    return;
                }
                break;
            case RECEIVED:
                if (targetStatus == ReturnMerchandiseStatus.COMPLETED || targetStatus == ReturnMerchandiseStatus.CANCELLED) {
                    return;
                }
                break;
            default:
                break;
        }

        throw new BadRequestException("Invalid RMA status transition from " + currentStatus + " to " + targetStatus);
    }

    private String generateShipmentNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String candidate;
        do {
            candidate = "SHP-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (shipmentRepository.existsByShipmentNumber(candidate));
        return candidate;
    }

    private String generateRmaNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String candidate;
        do {
            candidate = "RMA-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (rmaRepository.existsByRmaNumber(candidate));
        return candidate;
    }

    private String buildTrackingUrl(String carrier, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        if (carrier == null) {
            return "https://tracking.example.com/track/" + trackingNumber;
        }

        String normalized = carrier.trim().toLowerCase();
        if (normalized.contains("dhl")) {
            return "https://www.dhl.com/global-en/home/tracking/tracking-express.html?submit=1&tracking-id=" + trackingNumber;
        }
        if (normalized.contains("fedex")) {
            return "https://www.fedex.com/fedextrack/?trknbr=" + trackingNumber;
        }
        if (normalized.contains("ups")) {
            return "https://www.ups.com/track?tracknum=" + trackingNumber;
        }
        return "https://tracking.example.com/track/" + trackingNumber;
    }

    private String buildDeliveryNoteText(Shipment shipment) {
        String lines = shipment.getItems().stream()
                .map(item -> "- " + item.getProductVariant().getSku() + " : " + item.getQuantity())
                .collect(Collectors.joining("\n"));

        return "Delivery Note\n"
                + "Shipment: " + shipment.getShipmentNumber() + "\n"
                + "Sales Order: " + shipment.getSalesOrder().getSoNumber() + "\n"
                + "Carrier: " + (shipment.getCarrier() == null ? "N/A" : shipment.getCarrier()) + "\n"
                + "Tracking: " + (shipment.getTrackingNumber() == null ? "N/A" : shipment.getTrackingNumber()) + "\n"
                + "Shipped At: " + (shipment.getShippedDate() == null ? "N/A" : shipment.getShippedDate()) + "\n"
                + "Items:\n"
                + lines;
    }

    private ShipmentDto mapShipmentToDto(Shipment entity) {
        ShipmentDto dto = new ShipmentDto();
        dto.setId(entity.getId());
        dto.setShipmentNumber(entity.getShipmentNumber());
        dto.setSalesOrderId(entity.getSalesOrder().getId());
        dto.setSoNumber(entity.getSalesOrder().getSoNumber());
        dto.setWarehouseId(entity.getWarehouse().getId());
        dto.setWarehouseName(entity.getWarehouse().getName());
        dto.setStatus(entity.getStatus());
        dto.setCarrier(entity.getCarrier());
        dto.setCourierProvider(entity.getCourierProvider());
        dto.setCourierService(entity.getCourierService());
        dto.setCourierDispatchStatus(entity.getCourierDispatchStatus());
        dto.setCourierReference(entity.getCourierReference());
        dto.setTrackingNumber(entity.getTrackingNumber());
        dto.setTrackingUrl(entity.getTrackingUrl());
        dto.setShippingLabelUrl(entity.getShippingLabelUrl());
        dto.setCashOnDeliveryAmount(entity.getCashOnDeliveryAmount());
        dto.setDeliveryFee(entity.getDeliveryFee());
        dto.setShippedDate(entity.getShippedDate());
        dto.setDeliveredDate(entity.getDeliveredDate());
        dto.setDeliveryReviewStatus(entity.getDeliveryReviewStatus());
        dto.setDeliveryReviewReason(entity.getDeliveryReviewReason());
        dto.setDeliveryReviewRequestedAt(entity.getDeliveryReviewRequestedAt());
        dto.setDeliveryReviewResolvedAt(entity.getDeliveryReviewResolvedAt());
        dto.setProofOfDeliveryUrl(entity.getProofOfDeliveryUrl());
        dto.setProofOfDeliveryRecipientName(entity.getProofOfDeliveryRecipientName());
        dto.setProofOfDeliveryCapturedAt(entity.getProofOfDeliveryCapturedAt());
        dto.setPickupRequestedAt(entity.getPickupRequestedAt());
        dto.setPickedUpAt(entity.getPickedUpAt());
        dto.setOutForDeliveryAt(entity.getOutForDeliveryAt());
        dto.setLastCourierEvent(entity.getLastCourierEvent());
        dto.setLastCourierSyncAt(entity.getLastCourierSyncAt());
        dto.setDeliveryNote(entity.getDeliveryNote());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        List<ShipmentItemDto> itemDtos = entity.getItems().stream().map(item -> {
            ShipmentItemDto itemDto = new ShipmentItemDto();
            itemDto.setId(item.getId());
            itemDto.setSalesOrderItemId(item.getSalesOrderItem().getId());
            itemDto.setProductVariantId(item.getProductVariant().getId());
            itemDto.setSku(item.getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        List<ShipmentTimelineEventDto> timelineDtos = entity.getTimelineEvents().stream().map(event -> {
            ShipmentTimelineEventDto eventDto = new ShipmentTimelineEventDto();
            eventDto.setId(event.getId());
            eventDto.setEventType(event.getEventType());
            eventDto.setEventSource(event.getEventSource());
            eventDto.setSummary(event.getSummary());
            eventDto.setDetails(event.getDetails());
            eventDto.setEventAt(event.getEventAt());
            eventDto.setShipmentStatus(event.getShipmentStatus());
            eventDto.setCourierDispatchStatus(event.getCourierDispatchStatus());
            eventDto.setDeliveryReviewStatus(event.getDeliveryReviewStatus());
            eventDto.setCustomerVisible(event.isCustomerVisible());
            return eventDto;
        }).collect(Collectors.toList());
        dto.setTimeline(timelineDtos);

        return dto;
    }

    private void appendTimelineEventsForTrackingUpdate(
            Shipment shipment,
            ShipmentStatus previousStatus,
            CourierDispatchStatus previousDispatchStatus,
            DeliveryReviewStatus previousReviewStatus,
            String previousTrackingNumber,
            String previousCourierReference,
            UpdateShipmentTrackingRequest request
    ) {
        LocalDateTime eventAt = request.getLastCourierSyncAt() != null ? request.getLastCourierSyncAt() : LocalDateTime.now();
        String source = trimToNull(request.getTimelineSource());
        if (source == null) {
            source = request.getLastCourierSyncAt() != null ? "courier-sync" : "manual";
        }

        if (!Objects.equals(previousTrackingNumber, shipment.getTrackingNumber())
                || !Objects.equals(previousCourierReference, shipment.getCourierReference())) {
            appendTimelineEvent(
                    shipment,
                    ShipmentTimelineEventType.TRACKING_UPDATED,
                    source,
                    "Tracking details updated",
                    trimToNull(shipment.getLastCourierEvent()),
                    eventAt,
                    true
            );
        }

        if (previousDispatchStatus != shipment.getCourierDispatchStatus()) {
            appendTimelineEvent(
                    shipment,
                    ShipmentTimelineEventType.COURIER_STATUS_UPDATED,
                    source,
                    buildCourierStatusSummary(shipment.getCourierDispatchStatus()),
                    trimToNull(shipment.getLastCourierEvent()),
                    eventAt,
                    true
            );
        } else if (previousStatus != shipment.getStatus()) {
            appendTimelineEvent(
                    shipment,
                    ShipmentTimelineEventType.COURIER_STATUS_UPDATED,
                    source,
                    "Shipment status updated",
                    "Shipment status changed to " + shipment.getStatus(),
                    eventAt,
                    true
            );
        }

        if (previousReviewStatus != shipment.getDeliveryReviewStatus()) {
        ShipmentTimelineEventType eventType = isOpenDeliveryReview(shipment.getDeliveryReviewStatus())
                    ? ShipmentTimelineEventType.DELIVERY_REVIEW_REQUESTED
                    : ShipmentTimelineEventType.DELIVERY_REVIEW_RESOLVED;
            appendTimelineEvent(
                    shipment,
                    eventType,
                    source,
                    buildDeliveryReviewSummary(shipment.getDeliveryReviewStatus()),
                    trimToNull(shipment.getDeliveryReviewReason()),
                    eventAt,
                    false
            );
        }
    }

    private void applyDeliveryReviewUpdate(Shipment shipment, DeliveryReviewStatus requestedStatus, String requestedReason) {
        String normalizedReason = trimToNull(requestedReason);
        if (requestedStatus == null) {
            if (normalizedReason != null) {
                shipment.setDeliveryReviewReason(normalizedReason);
            }
            return;
        }

        DeliveryReviewStatus previousStatus = shipment.getDeliveryReviewStatus();
        shipment.setDeliveryReviewStatus(requestedStatus);
        if (normalizedReason != null || requestedStatus == DeliveryReviewStatus.NOT_REQUIRED) {
            shipment.setDeliveryReviewReason(normalizedReason);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean previousOpenReview = isOpenDeliveryReview(previousStatus);
        boolean requestedOpenReview = isOpenDeliveryReview(requestedStatus);

        if (requestedOpenReview && !previousOpenReview) {
            shipment.setDeliveryReviewRequestedAt(shipment.getDeliveryReviewRequestedAt() != null ? shipment.getDeliveryReviewRequestedAt() : now);
            shipment.setDeliveryReviewResolvedAt(null);
        }
        if (requestedOpenReview && previousOpenReview) {
            shipment.setDeliveryReviewResolvedAt(null);
        }
        if (!requestedOpenReview && previousOpenReview) {
            shipment.setDeliveryReviewResolvedAt(now);
        }
        if (requestedStatus == DeliveryReviewStatus.NOT_REQUIRED && !previousOpenReview) {
            shipment.setDeliveryReviewResolvedAt(null);
        }
    }

    private boolean isOpenDeliveryReview(DeliveryReviewStatus status) {
        return status == DeliveryReviewStatus.PENDING || status == DeliveryReviewStatus.DISPUTED;
    }

    private void appendTimelineEvent(
            Shipment shipment,
            ShipmentTimelineEventType eventType,
            String source,
            String summary,
            String details,
            LocalDateTime eventAt,
            boolean customerVisible
    ) {
        ShipmentTimelineEvent event = new ShipmentTimelineEvent();
        event.setShipment(shipment);
        event.setEventType(eventType);
        event.setEventSource(trimToNull(source));
        event.setSummary(summary);
        event.setDetails(trimToNull(details));
        event.setEventAt(eventAt != null ? eventAt : LocalDateTime.now());
        event.setShipmentStatus(shipment.getStatus());
        event.setCourierDispatchStatus(shipment.getCourierDispatchStatus());
        event.setDeliveryReviewStatus(shipment.getDeliveryReviewStatus());
        event.setCustomerVisible(customerVisible);
        shipment.getTimelineEvents().add(event);
    }

    private String buildCourierStatusSummary(CourierDispatchStatus status) {
        if (status == null) {
            return "Courier status updated";
        }
        return switch (status) {
            case UNASSIGNED -> "Shipment waiting for courier assignment";
            case BOOKED -> "Courier booking confirmed";
            case PICKUP_PENDING -> "Awaiting courier pickup";
            case PICKED_UP -> "Shipment picked up by courier";
            case IN_TRANSIT -> "Shipment is in transit";
            case OUT_FOR_DELIVERY -> "Shipment is out for delivery";
            case DELIVERED -> "Courier marked shipment delivered";
            case DELIVERY_FAILED -> "Courier reported a failed delivery attempt";
            case RETURNED -> "Shipment returned by courier";
            case CANCELLED -> "Shipment cancelled by courier";
        };
    }

    private String buildDeliveryReviewSummary(DeliveryReviewStatus status) {
        if (status == null) {
            return "Delivery review updated";
        }
        return switch (status) {
            case NOT_REQUIRED -> "Delivery review cleared";
            case PENDING -> "Delivery requires internal review";
            case APPROVED -> "Delivery review approved";
            case DISPUTED -> "Delivery marked as disputed";
        };
    }

    private String buildProofOfDeliveryDetails(Shipment shipment) {
        List<String> parts = new ArrayList<>();
        if (shipment.getProofOfDeliveryRecipientName() != null) {
            parts.add("Recipient: " + shipment.getProofOfDeliveryRecipientName());
        }
        if (shipment.getProofOfDeliveryUrl() != null) {
            parts.add("Proof URL: " + shipment.getProofOfDeliveryUrl());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private void applyCourierState(Shipment shipment) {
        CourierDispatchStatus dispatchStatus = shipment.getCourierDispatchStatus();
        if (dispatchStatus == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        switch (dispatchStatus) {
            case BOOKED -> {
                if (shipment.getPickupRequestedAt() == null) {
                    shipment.setPickupRequestedAt(now);
                }
                if (shipment.getStatus() == ShipmentStatus.DRAFT) {
                    shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
                }
            }
            case PICKUP_PENDING -> {
                if (shipment.getPickupRequestedAt() == null) {
                    shipment.setPickupRequestedAt(now);
                }
                shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
            }
            case PICKED_UP, IN_TRANSIT -> {
                if (shipment.getPickedUpAt() == null) {
                    shipment.setPickedUpAt(now);
                }
                if (shipment.getShippedDate() == null) {
                    shipment.setShippedDate(now);
                }
                shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            }
            case OUT_FOR_DELIVERY -> {
                if (shipment.getOutForDeliveryAt() == null) {
                    shipment.setOutForDeliveryAt(now);
                }
                if (shipment.getShippedDate() == null) {
                    shipment.setShippedDate(now);
                }
                shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            }
            case DELIVERED -> {
                shipment.setStatus(ShipmentStatus.DELIVERED);
                if (shipment.getShippedDate() == null) {
                    shipment.setShippedDate(now);
                }
                if (shipment.getDeliveredDate() == null) {
                    shipment.setDeliveredDate(now);
                }
            }
            case DELIVERY_FAILED -> shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            case RETURNED -> shipment.setStatus(ShipmentStatus.RETURNED);
            case CANCELLED -> shipment.setStatus(ShipmentStatus.CANCELLED);
            case UNASSIGNED -> shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
            default -> {
            }
        }
    }

    private void synchronizeSalesOrderDeliveryStatus(SalesOrder salesOrder) {
        List<Shipment> orderShipments = shipmentRepository.findBySalesOrderId(salesOrder.getId());
        boolean allDelivered = !orderShipments.isEmpty() && orderShipments.stream().allMatch(shipment -> shipment.getStatus() == ShipmentStatus.DELIVERED);
        if (allDelivered) {
            salesOrder.setStatus(SalesOrderStatus.DELIVERED);
        } else {
            updateSalesOrderShipmentStatus(salesOrder);
        }
        salesOrderRepository.save(salesOrder);
    }

    private Specification<Shipment> buildShipmentQueueSpecification(ShipmentQueueType queue) {
        return (root, query, cb) -> buildShipmentQueuePredicate(root, cb, queue);
    }

    private Predicate buildShipmentQueuePredicate(Root<Shipment> root, CriteriaBuilder cb, ShipmentQueueType queue) {
        Path<ShipmentStatus> shipmentStatus = root.get("status");
        Path<CourierDispatchStatus> dispatchStatus = root.get("courierDispatchStatus");
        Path<DeliveryReviewStatus> reviewStatus = root.get("deliveryReviewStatus");

        jakarta.persistence.criteria.Expression<String> courierProvider = cb.trim(cb.coalesce(root.get("courierProvider"), ""));
        jakarta.persistence.criteria.Expression<String> courierReference = cb.trim(cb.coalesce(root.get("courierReference"), ""));

        Predicate providerMissing = cb.equal(courierProvider, "");
        Predicate referenceMissing = cb.equal(courierReference, "");
        Predicate openReview = reviewStatus.in(DeliveryReviewStatus.PENDING, DeliveryReviewStatus.DISPUTED);
        Predicate hardCourierException = dispatchStatus.in(
                CourierDispatchStatus.DELIVERY_FAILED,
                CourierDispatchStatus.RETURNED,
                CourierDispatchStatus.CANCELLED
        );
        Predicate missingHandoffData = cb.and(cb.equal(shipmentStatus, ShipmentStatus.READY_TO_SHIP), providerMissing);
        Predicate bookedWithoutReference = cb.and(
                dispatchStatus.in(CourierDispatchStatus.BOOKED, CourierDispatchStatus.PICKUP_PENDING),
                cb.not(providerMissing),
                referenceMissing
        );
        Predicate needsAction = cb.or(openReview, hardCourierException, missingHandoffData, bookedWithoutReference);

        return switch (queue) {
            case READY_TO_HANDOFF -> cb.and(
                    cb.equal(shipmentStatus, ShipmentStatus.READY_TO_SHIP),
                    dispatchStatus.in(CourierDispatchStatus.UNASSIGNED, CourierDispatchStatus.BOOKED, CourierDispatchStatus.PICKUP_PENDING),
                    cb.not(providerMissing),
                    cb.not(bookedWithoutReference)
            );
            case IN_TRANSIT -> dispatchStatus.in(
                    CourierDispatchStatus.PICKED_UP,
                    CourierDispatchStatus.IN_TRANSIT,
                    CourierDispatchStatus.OUT_FOR_DELIVERY
            );
            case NEEDS_ACTION -> needsAction;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RmaDto mapRmaToDto(ReturnMerchandiseAuthorization entity) {
        RmaDto dto = new RmaDto();
        dto.setId(entity.getId());
        dto.setRmaNumber(entity.getRmaNumber());
        dto.setSalesOrderId(entity.getSalesOrder().getId());
        dto.setSoNumber(entity.getSalesOrder().getSoNumber());
        if (entity.getShipment() != null) {
            dto.setShipmentId(entity.getShipment().getId());
            dto.setShipmentNumber(entity.getShipment().getShipmentNumber());
        }
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setNotes(entity.getNotes());
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setReceivedAt(entity.getReceivedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        List<RmaItemDto> itemDtos = entity.getItems().stream().map(item -> {
            RmaItemDto itemDto = new RmaItemDto();
            itemDto.setId(item.getId());
            itemDto.setSalesOrderItemId(item.getSalesOrderItem().getId());
            itemDto.setProductVariantId(item.getProductVariant().getId());
            itemDto.setSku(item.getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setReason(item.getReason());
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }
}
