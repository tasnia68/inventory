package com.inventory.system.service.impl;

import com.inventory.system.common.entity.ReturnMerchandiseAuthorization;
import com.inventory.system.common.entity.ReturnMerchandiseItem;
import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import com.inventory.system.common.entity.PickingStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShipmentItem;
import com.inventory.system.common.entity.ShipmentStatus;
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
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentItemDto;
import com.inventory.system.payload.ShipmentSearchRequest;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.payload.UpdateShipmentTrackingRequest;
import com.inventory.system.repository.ReturnMerchandiseAuthorizationRepository;
import com.inventory.system.repository.ReturnMerchandiseItemRepository;
import com.inventory.system.repository.PickingTaskRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.repository.ShipmentItemRepository;
import com.inventory.system.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setSalesOrder(salesOrder);
        shipment.setWarehouse(salesOrder.getWarehouse());
        shipment.setCarrier(request.getCarrier());
        shipment.setNotes(request.getNotes());
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setShippedDate(LocalDateTime.now());

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
    public ShipmentDto updateTracking(UUID shipmentId, UpdateShipmentTrackingRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new BadRequestException("Tracking cannot be updated for closed shipments");
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
        if (request.getStatus() != null) {
            shipment.setStatus(request.getStatus());
        }

        Shipment saved = shipmentRepository.save(shipment);
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

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredDate(request != null && request.getDeliveredAt() != null ? request.getDeliveredAt() : LocalDateTime.now());

        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            shipment.setNotes(request.getNotes());
        }

        Shipment savedShipment = shipmentRepository.save(shipment);

        SalesOrder salesOrder = savedShipment.getSalesOrder();
        List<Shipment> orderShipments = shipmentRepository.findBySalesOrderId(salesOrder.getId());
        boolean allDelivered = !orderShipments.isEmpty() && orderShipments.stream().allMatch(s -> s.getStatus() == ShipmentStatus.DELIVERED);
        if (allDelivered) {
            salesOrder.setStatus(SalesOrderStatus.DELIVERED);
            salesOrderRepository.save(salesOrder);
        }

        return mapShipmentToDto(savedShipment);
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
        dto.setTrackingNumber(entity.getTrackingNumber());
        dto.setTrackingUrl(entity.getTrackingUrl());
        dto.setShippingLabelUrl(entity.getShippingLabelUrl());
        dto.setShippedDate(entity.getShippedDate());
        dto.setDeliveredDate(entity.getDeliveredDate());
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

        return dto;
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