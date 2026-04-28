package com.inventory.system.service.impl;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
import com.inventory.system.service.PricingEngineService;
import com.inventory.system.service.PricingEvaluation;
import com.inventory.system.service.PricingEvaluationLine;
import com.inventory.system.service.SalesOrderService;
import com.inventory.system.service.StockReservationService;
import com.inventory.system.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockReservationService stockReservationService;
    private final WarehouseRepository warehouseRepository;
    private final PricingEngineService pricingEngineService;
    private final PromotionRedemptionRepository promotionRedemptionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ShipmentRepository shipmentRepository;
    private final StockService stockService;

    @Override
    @Transactional
    public SalesOrderDto createSalesOrder(SalesOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        validateCustomerEligibility(customer);

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + request.getWarehouseId()));

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomer(customer);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        salesOrder.setCurrency(request.getCurrency());
        salesOrder.setNotes(request.getNotes());
        salesOrder.setOrderDate(LocalDateTime.now());
        salesOrder.setStatus(SalesOrderStatus.DRAFT);
        salesOrder.setPriority(request.getPriority() != null ? request.getPriority() : OrderPriority.MEDIUM);
        salesOrder.setSoNumber(generateSoNumber());
        salesOrder.setSalesChannel(SalesChannel.SALES_ORDER);

        Set<UUID> variantIds = request.getItems().stream()
                .map(SalesOrderItemRequest::getProductVariantId)
                .collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("Some product variants not found");
        }

        PricingEvaluation pricingEvaluation = pricingEngineService.evaluateSalesOrder(customer, warehouse, request);
        Map<UUID, Deque<PricingEvaluationLine>> pricedLines = buildLineQueues(pricingEvaluation);

        List<SalesOrderItem> items = new ArrayList<>();

        for (SalesOrderItemRequest itemRequest : request.getItems()) {
            ProductVariant productVariant = variantMap.get(itemRequest.getProductVariantId());
            PricingEvaluationLine pricedLine = popPricedLine(pricedLines, productVariant.getId());

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(salesOrder);
            item.setProductVariant(productVariant);
            item.setQuantity(itemRequest.getQuantity());
            item.setBaseUnitPrice(pricedLine.getBaseUnitPrice());
            item.setUnitPrice(pricedLine.getFinalUnitPrice());
            item.setLineDiscount(pricedLine.getLineDiscountAmount());
            item.setAppliedPromotionCodes(String.join(", ", pricedLine.getAppliedPromotionCodes()));
            item.setTotalPrice(pricedLine.getLineTotalAmount());
            item.setShippedQuantity(BigDecimal.ZERO);

            items.add(item);
        }

        salesOrder.setItems(items);
        salesOrder.setSubtotalAmount(pricingEvaluation.getBaseSubtotal());
        salesOrder.setDiscountAmount(pricingEvaluation.getTotalDiscount());
        salesOrder.setTotalAmount(pricingEvaluation.getNetSubtotal());
        salesOrder.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));

        SalesOrder savedSo = salesOrderRepository.save(salesOrder);
        pricingEngineService.recordRedemptions(pricingEvaluation, savedSo, null, customer, SalesChannel.SALES_ORDER, savedSo.getSoNumber());
        return mapToDto(savedSo);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(UUID id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));
        return mapToDto(salesOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderDto> getAllSalesOrders(SalesOrderSearchRequest searchRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        Specification<SalesOrder> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (searchRequest.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), searchRequest.getCustomerId()));
            }
            if (searchRequest.getWarehouseId() != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), searchRequest.getWarehouseId()));
            }
            if (searchRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchRequest.getStatus()));
            }
            if (searchRequest.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), searchRequest.getPriority()));
            }
            if (searchRequest.getSoNumber() != null && !searchRequest.getSoNumber().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("soNumber")), "%" + searchRequest.getSoNumber().toUpperCase() + "%"));
            }
             if (searchRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), searchRequest.getStartDate().atStartOfDay()));
            }
            if (searchRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), searchRequest.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<SalesOrder> salesOrders = salesOrderRepository.findAll(spec, pageable);
        return salesOrders.map(this::mapToDto);
    }

    @Override
    @Transactional
    public SalesOrderDto updateSalesOrder(UUID id, SalesOrderRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));

        if (salesOrder.getStatus() != SalesOrderStatus.DRAFT && salesOrder.getStatus() != SalesOrderStatus.PENDING) {
            throw new BadRequestException("Cannot update Sales Order that is already confirmed or processed");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));
        validateCustomerEligibility(customer);
        salesOrder.setCustomer(customer);

        promotionRedemptionRepository.deleteBySalesOrderId(salesOrder.getId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + request.getWarehouseId()));
        salesOrder.setWarehouse(warehouse);

        salesOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        salesOrder.setCurrency(request.getCurrency());
        salesOrder.setNotes(request.getNotes());
        salesOrder.setPriority(request.getPriority() != null ? request.getPriority() : OrderPriority.MEDIUM);

        salesOrder.getItems().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Set<UUID> variantIds = request.getItems().stream()
                .map(SalesOrderItemRequest::getProductVariantId)
                .collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("Some product variants not found");
        }

        PricingEvaluation pricingEvaluation = pricingEngineService.evaluateSalesOrder(customer, warehouse, request);
        Map<UUID, Deque<PricingEvaluationLine>> pricedLines = buildLineQueues(pricingEvaluation);

        for (SalesOrderItemRequest itemRequest : request.getItems()) {
            ProductVariant productVariant = variantMap.get(itemRequest.getProductVariantId());
            PricingEvaluationLine pricedLine = popPricedLine(pricedLines, productVariant.getId());

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(salesOrder);
            item.setProductVariant(productVariant);
            item.setQuantity(itemRequest.getQuantity());
            item.setBaseUnitPrice(pricedLine.getBaseUnitPrice());
            item.setUnitPrice(pricedLine.getFinalUnitPrice());
            item.setLineDiscount(pricedLine.getLineDiscountAmount());
            item.setAppliedPromotionCodes(String.join(", ", pricedLine.getAppliedPromotionCodes()));
            item.setTotalPrice(pricedLine.getLineTotalAmount());
            item.setShippedQuantity(BigDecimal.ZERO);

            salesOrder.getItems().add(item);
        }
        salesOrder.setSalesChannel(SalesChannel.SALES_ORDER);
        salesOrder.setSubtotalAmount(pricingEvaluation.getBaseSubtotal());
        salesOrder.setDiscountAmount(pricingEvaluation.getTotalDiscount());
        salesOrder.setTotalAmount(pricingEvaluation.getNetSubtotal());
        salesOrder.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));

        SalesOrder savedSo = salesOrderRepository.save(salesOrder);
        pricingEngineService.recordRedemptions(pricingEvaluation, savedSo, null, customer, SalesChannel.SALES_ORDER, savedSo.getSoNumber());
        return mapToDto(savedSo);
    }

    @Override
    @Transactional
    public SalesOrderDto updateSalesOrderStatus(UUID id, SalesOrderStatus status) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));

        SalesOrderStatus previousStatus = salesOrder.getStatus();
        validateStatusTransition(previousStatus, status);

        SalesOrderStatus effectiveTarget = status;

        if (needsReservation(previousStatus, status) && !hasActiveReservation(previousStatus)) {
            boolean hasBackorder = reserveForOrder(salesOrder);
            if (status == SalesOrderStatus.CONFIRMED && hasBackorder) {
                effectiveTarget = SalesOrderStatus.BACKORDERED;
            }
        }

        if (status == SalesOrderStatus.CONFIRMED && previousStatus != SalesOrderStatus.CONFIRMED) {
            validateCustomerEligibility(salesOrder.getCustomer());
            validateCustomerCredit(salesOrder);
        }

        if (status == SalesOrderStatus.SHIPPED && previousStatus != SalesOrderStatus.SHIPPED) {
            stockReservationService.fulfillReservationsByReference(salesOrder.getSoNumber());
            relieveInventoryOnShip(salesOrder);
        }

        if (previousStatus == SalesOrderStatus.PACKAGING && status == SalesOrderStatus.SHIPPED) {
            salesOrder.setPackagingCompletedAt(LocalDateTime.now());
        }

        if (status == SalesOrderStatus.CANCELLED && previousStatus != SalesOrderStatus.CANCELLED) {
            stockReservationService.releaseReservationsByReference(salesOrder.getSoNumber());
        }

        salesOrder.setStatus(effectiveTarget);

        if (effectiveTarget == SalesOrderStatus.RETURNED) {
            restockFullReturn(salesOrder);
        }

        SalesOrder savedSo = salesOrderRepository.save(salesOrder);
        eventPublisher.publishEvent(new com.inventory.system.service.order.events.OrderStatusChangedEvent(
                savedSo.getId(), previousStatus, effectiveTarget, LocalDateTime.now()));
        return mapToDto(savedSo);
    }

    private void restockFullReturn(SalesOrder salesOrder) {
        if (salesOrder.getWarehouse() == null) return;
        for (SalesOrderItem item : salesOrder.getItems()) {
            BigDecimal ordered = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal alreadyReturned = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal delta = ordered.subtract(alreadyReturned);
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                restockItem(salesOrder, item, delta);
                item.setReturnedQuantity(ordered);
            }
        }
    }

    private void relieveInventoryOnShip(SalesOrder salesOrder) {
        if (salesOrder.getWarehouse() == null) return;
        for (SalesOrderItem item : salesOrder.getItems()) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal alreadyShipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
            BigDecimal delta = qty.subtract(alreadyShipped);
            if (delta.compareTo(BigDecimal.ZERO) <= 0) continue;
            StockAdjustmentDto adjustment = new StockAdjustmentDto();
            adjustment.setProductVariantId(item.getProductVariant().getId());
            adjustment.setWarehouseId(salesOrder.getWarehouse().getId());
            adjustment.setQuantity(delta);
            adjustment.setType(com.inventory.system.common.entity.StockMovement.StockMovementType.OUT);
            adjustment.setReason("Inventory relief for SO " + salesOrder.getSoNumber());
            adjustment.setReferenceId(salesOrder.getId().toString() + ":ship:" + item.getId());
            stockService.adjustStock(adjustment);
            item.setShippedQuantity(qty);
        }
    }

    private void restockItem(SalesOrder salesOrder, SalesOrderItem item, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) return;
        StockAdjustmentDto adjustment = new StockAdjustmentDto();
        adjustment.setProductVariantId(item.getProductVariant().getId());
        adjustment.setWarehouseId(salesOrder.getWarehouse().getId());
        adjustment.setQuantity(quantity);
        adjustment.setType(com.inventory.system.common.entity.StockMovement.StockMovementType.IN);
        adjustment.setReason("Restock on return for SO " + salesOrder.getSoNumber());
        adjustment.setReferenceId(salesOrder.getId().toString() + ":return:" + item.getId());
        stockService.adjustStock(adjustment);
    }

    private boolean needsReservation(SalesOrderStatus previousStatus, SalesOrderStatus targetStatus) {
        return targetStatus == SalesOrderStatus.HOLD
                || (targetStatus == SalesOrderStatus.APPROVED && previousStatus != SalesOrderStatus.HOLD)
                || (targetStatus == SalesOrderStatus.CONFIRMED
                    && previousStatus != SalesOrderStatus.HOLD
                    && previousStatus != SalesOrderStatus.APPROVED
                    && previousStatus != SalesOrderStatus.BACKORDERED);
    }

    private boolean hasActiveReservation(SalesOrderStatus previousStatus) {
        return previousStatus == SalesOrderStatus.HOLD
                || previousStatus == SalesOrderStatus.APPROVED
                || previousStatus == SalesOrderStatus.CONFIRMED
                || previousStatus == SalesOrderStatus.PACKAGING
                || previousStatus == SalesOrderStatus.BACKORDERED;
    }

    private boolean reserveForOrder(SalesOrder salesOrder) {
        if (salesOrder.getWarehouse() == null) {
            return false;
        }
        boolean hasBackorder = false;
        for (SalesOrderItem item : salesOrder.getItems()) {
            BigDecimal available = stockReservationService.getAvailableToPromise(
                    item.getProductVariant().getId(), salesOrder.getWarehouse().getId());
            BigDecimal reservableQuantity = available.min(item.getQuantity());
            if (reservableQuantity.compareTo(BigDecimal.ZERO) > 0) {
                StockReservationRequest reservationRequest = new StockReservationRequest();
                reservationRequest.setProductVariantId(item.getProductVariant().getId());
                reservationRequest.setWarehouseId(salesOrder.getWarehouse().getId());
                reservationRequest.setQuantity(reservableQuantity);
                reservationRequest.setReferenceId(salesOrder.getSoNumber());
                reservationRequest.setNotes("Reservation for Sales Order " + salesOrder.getSoNumber());
                reservationRequest.setPriority(mapToReservationPriority(salesOrder.getPriority()));
                stockReservationService.reserveStock(reservationRequest);
            }
            if (available.compareTo(item.getQuantity()) < 0) {
                hasBackorder = true;
            }
        }
        return hasBackorder;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<SalesOrderStatus> getAllowedTransitions(UUID id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));
        return allowedTransitionsFrom(salesOrder.getStatus());
    }

    @Override
    @Transactional
    public SalesOrderDto holdOrder(UUID id, String reason) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));
        salesOrder.setHoldReason(reason);
        salesOrderRepository.save(salesOrder);
        return updateSalesOrderStatus(id, SalesOrderStatus.HOLD);
    }

    @Override
    @Transactional
    public SalesOrderDto confirmOrder(UUID id, com.inventory.system.payload.ConfirmOrderRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));
        if (request != null) {
            if (request.getCourierProfileId() != null) {
                salesOrder.setCourierProfileId(request.getCourierProfileId());
            }
            if (request.getDeliveryZone() != null) {
                salesOrder.setDeliveryZone(request.getDeliveryZone());
            }
        }
        salesOrderRepository.save(salesOrder);
        return updateSalesOrderStatus(id, SalesOrderStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public SalesOrderDto partialDeliver(UUID id, java.util.List<com.inventory.system.payload.PartialDeliveryLineRequest> lines) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));

        Map<UUID, SalesOrderItem> itemsById = salesOrder.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        boolean anyReturned = false;
        boolean anyCancelled = false;
        BigDecimal totalFulfilled = BigDecimal.ZERO;
        BigDecimal totalOrdered = BigDecimal.ZERO;

        for (com.inventory.system.payload.PartialDeliveryLineRequest line : lines) {
            SalesOrderItem item = itemsById.get(line.getItemId());
            if (item == null) {
                throw new BadRequestException("Unknown order item: " + line.getItemId());
            }
            BigDecimal ordered = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal fulfilled = line.getFulfilledQuantity() != null ? line.getFulfilledQuantity() : BigDecimal.ZERO;
            BigDecimal returned = line.getReturnedQuantity() != null ? line.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal cancelled = line.getCancelledQuantity() != null ? line.getCancelledQuantity() : BigDecimal.ZERO;
            BigDecimal sum = fulfilled.add(returned).add(cancelled);
            if (sum.compareTo(ordered) > 0) {
                throw new BadRequestException("Line " + item.getId() + ": fulfilled+returned+cancelled exceeds ordered quantity");
            }
            BigDecimal priorReturned = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal priorCancelled = item.getCancelledQuantity() != null ? item.getCancelledQuantity() : BigDecimal.ZERO;
            BigDecimal returnDelta = returned.subtract(priorReturned);
            BigDecimal cancelDelta = cancelled.subtract(priorCancelled);
            item.setFulfilledQuantity(fulfilled);
            item.setReturnedQuantity(returned);
            item.setCancelledQuantity(cancelled);
            BigDecimal restockDelta = returnDelta.max(BigDecimal.ZERO).add(cancelDelta.max(BigDecimal.ZERO));
            if (restockDelta.compareTo(BigDecimal.ZERO) > 0) {
                restockItem(salesOrder, item, restockDelta);
            }
            totalOrdered = totalOrdered.add(ordered);
            totalFulfilled = totalFulfilled.add(fulfilled);
            if (returned.compareTo(BigDecimal.ZERO) > 0) anyReturned = true;
            if (cancelled.compareTo(BigDecimal.ZERO) > 0) anyCancelled = true;
        }

        SalesOrderStatus target;
        if (totalFulfilled.compareTo(BigDecimal.ZERO) == 0 && anyCancelled) {
            target = SalesOrderStatus.PARTIALLY_CANCELLED;
        } else if (totalFulfilled.compareTo(totalOrdered) == 0 && !anyReturned && !anyCancelled) {
            target = SalesOrderStatus.DELIVERED;
        } else {
            target = SalesOrderStatus.PARTIALLY_DELIVERED;
        }

        salesOrderRepository.save(salesOrder);
        return updateSalesOrderStatus(id, target);
    }

    @Override
    @Transactional
    public SalesOrderDto updateItems(UUID id, List<SalesOrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BadRequestException("At least one item is required");
        }

        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));

        SalesOrderStatus status = salesOrder.getStatus();
        Set<SalesOrderStatus> blocked = EnumSet.of(
                SalesOrderStatus.SHIPPED, SalesOrderStatus.DELIVERED,
                SalesOrderStatus.PARTIALLY_DELIVERED, SalesOrderStatus.PARTIALLY_CANCELLED,
                SalesOrderStatus.RETURNED, SalesOrderStatus.CANCELLED,
                SalesOrderStatus.DELIVERY_FAILED);
        if (blocked.contains(status)) {
            throw new BadRequestException("Cannot edit items when order is in " + status);
        }

        List<com.inventory.system.common.entity.Shipment> bookedShipments = shipmentRepository
                .findBySalesOrderId(salesOrder.getId()).stream()
                .filter(s -> s.getCourierReference() != null && !s.getCourierReference().isBlank())
                .toList();
        if (!bookedShipments.isEmpty()) {
            throw new BadRequestException(
                    "Cancel the courier booking before editing items; current provider (" +
                            bookedShipments.get(0).getCourierProvider() + ") does not support updates after booking.");
        }

        boolean hadReservation = hasActiveReservation(status);
        if (hadReservation) {
            stockReservationService.releaseReservationsByReference(salesOrder.getSoNumber());
        }

        Set<UUID> variantIds = itemRequests.stream()
                .map(SalesOrderItemRequest::getProductVariantId)
                .collect(Collectors.toSet());
        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("Some product variants not found");
        }

        SalesOrderRequest syntheticRequest = new SalesOrderRequest();
        syntheticRequest.setCustomerId(salesOrder.getCustomer().getId());
        syntheticRequest.setWarehouseId(salesOrder.getWarehouse() != null ? salesOrder.getWarehouse().getId() : null);
        syntheticRequest.setItems(itemRequests);
        syntheticRequest.setCurrency(salesOrder.getCurrency());

        PricingEvaluation eval = pricingEngineService.evaluateSalesOrder(
                salesOrder.getCustomer(), salesOrder.getWarehouse(), syntheticRequest);
        Map<UUID, Deque<PricingEvaluationLine>> pricedLines = buildLineQueues(eval);

        promotionRedemptionRepository.deleteBySalesOrderId(salesOrder.getId());
        salesOrder.getItems().clear();

        for (SalesOrderItemRequest itemReq : itemRequests) {
            ProductVariant variant = variantMap.get(itemReq.getProductVariantId());
            PricingEvaluationLine pricedLine = popPricedLine(pricedLines, variant.getId());
            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(salesOrder);
            item.setProductVariant(variant);
            item.setQuantity(itemReq.getQuantity());
            item.setBaseUnitPrice(pricedLine.getBaseUnitPrice());
            item.setUnitPrice(pricedLine.getFinalUnitPrice());
            item.setLineDiscount(pricedLine.getLineDiscountAmount());
            item.setAppliedPromotionCodes(String.join(", ", pricedLine.getAppliedPromotionCodes()));
            item.setTotalPrice(pricedLine.getLineTotalAmount());
            item.setShippedQuantity(BigDecimal.ZERO);
            item.setFulfilledQuantity(BigDecimal.ZERO);
            item.setReturnedQuantity(BigDecimal.ZERO);
            item.setCancelledQuantity(BigDecimal.ZERO);
            salesOrder.getItems().add(item);
        }

        salesOrder.setSubtotalAmount(eval.getBaseSubtotal());
        salesOrder.setDiscountAmount(eval.getTotalDiscount());
        salesOrder.setTotalAmount(eval.getNetSubtotal());
        salesOrder.setAppliedCouponCodes(String.join(", ", eval.getAppliedCouponCodes()));

        SalesOrder saved = salesOrderRepository.save(salesOrder);
        pricingEngineService.recordRedemptions(eval, saved, null, saved.getCustomer(),
                saved.getSalesChannel(), saved.getSoNumber());

        if (hadReservation) {
            reserveForOrder(saved);
        }
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void cancelSalesOrder(UUID id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with ID: " + id));

        if (salesOrder.getStatus() == SalesOrderStatus.SHIPPED || salesOrder.getStatus() == SalesOrderStatus.DELIVERED) {
             throw new BadRequestException("Cannot cancel Sales Order that has been shipped or delivered");
        }

        stockReservationService.releaseReservationsByReference(salesOrder.getSoNumber());
        promotionRedemptionRepository.deleteBySalesOrderId(salesOrder.getId());

        salesOrder.setStatus(SalesOrderStatus.CANCELLED);
        salesOrderRepository.save(salesOrder);
    }

    private ReservationPriority mapToReservationPriority(OrderPriority orderPriority) {
        if (orderPriority == null) return ReservationPriority.MEDIUM;
        switch (orderPriority) {
            case LOW: return ReservationPriority.LOW;
            case MEDIUM: return ReservationPriority.MEDIUM;
            case HIGH: return ReservationPriority.HIGH;
            case URGENT: return ReservationPriority.CRITICAL;
            default: return ReservationPriority.MEDIUM;
        }
    }

    private void validateCustomerEligibility(Customer customer) {
        if (customer.getIsActive() != null && !customer.getIsActive()) {
            throw new BadRequestException("Customer is inactive and cannot be used for sales orders");
        }
        if (customer.getStatus() == CustomerStatus.BLOCKED || customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BadRequestException("Customer status does not allow sales orders: " + customer.getStatus());
        }
    }

    private void validateCustomerCredit(SalesOrder salesOrder) {
        Customer customer = salesOrder.getCustomer();
        if (customer.getCreditLimit() == null) {
            return;
        }

        BigDecimal outstandingBalance = customer.getOutstandingBalance() != null
                ? customer.getOutstandingBalance()
                : BigDecimal.ZERO;
        BigDecimal projectedBalance = outstandingBalance.add(salesOrder.getTotalAmount());
        if (projectedBalance.compareTo(customer.getCreditLimit()) > 0) {
            throw new BadRequestException(
                    "Customer credit limit exceeded. Limit: " + customer.getCreditLimit() + ", projected exposure: " + projectedBalance
            );
        }
    }

    private static final Map<SalesOrderStatus, Set<SalesOrderStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(SalesOrderStatus.DRAFT, EnumSet.of(
                    SalesOrderStatus.PENDING, SalesOrderStatus.APPROVED,
                    SalesOrderStatus.CONFIRMED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.PENDING, EnumSet.of(
                    SalesOrderStatus.DRAFT, SalesOrderStatus.HOLD,
                    SalesOrderStatus.APPROVED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.HOLD, EnumSet.of(
                    SalesOrderStatus.PENDING, SalesOrderStatus.APPROVED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.APPROVED, EnumSet.of(
                    SalesOrderStatus.CONFIRMED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.CONFIRMED, EnumSet.of(
                    SalesOrderStatus.PACKAGING, SalesOrderStatus.BACKORDERED,
                    SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.PACKAGING, EnumSet.of(
                    SalesOrderStatus.CONFIRMED, SalesOrderStatus.SHIPPED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.BACKORDERED, EnumSet.of(
                    SalesOrderStatus.CONFIRMED, SalesOrderStatus.PACKAGING, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.PARTIALLY_SHIPPED, EnumSet.of(
                    SalesOrderStatus.SHIPPED, SalesOrderStatus.DELIVERED,
                    SalesOrderStatus.PARTIALLY_DELIVERED, SalesOrderStatus.CANCELLED)),
            Map.entry(SalesOrderStatus.SHIPPED, EnumSet.of(
                    SalesOrderStatus.DELIVERED, SalesOrderStatus.PARTIALLY_DELIVERED,
                    SalesOrderStatus.PARTIALLY_CANCELLED, SalesOrderStatus.CANCELLED,
                    SalesOrderStatus.DELIVERY_FAILED, SalesOrderStatus.RETURNED)),
            Map.entry(SalesOrderStatus.DELIVERED, EnumSet.of(
                    SalesOrderStatus.RETURNED, SalesOrderStatus.PARTIALLY_DELIVERED)),
            Map.entry(SalesOrderStatus.PARTIALLY_DELIVERED, EnumSet.of(
                    SalesOrderStatus.RETURNED, SalesOrderStatus.DELIVERED)),
            Map.entry(SalesOrderStatus.PARTIALLY_CANCELLED, EnumSet.of(
                    SalesOrderStatus.RETURNED, SalesOrderStatus.DELIVERED)),
            Map.entry(SalesOrderStatus.DELIVERY_FAILED, EnumSet.of(
                    SalesOrderStatus.SHIPPED, SalesOrderStatus.CANCELLED, SalesOrderStatus.RETURNED))
    );

    private void validateStatusTransition(SalesOrderStatus currentStatus, SalesOrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        Set<SalesOrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(SalesOrderStatus.class));
        if (!allowed.contains(targetStatus)) {
            throw new BadRequestException("Invalid sales order status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    public static Set<SalesOrderStatus> allowedTransitionsFrom(SalesOrderStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(SalesOrderStatus.class));
    }

    private String generateSoNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SO-" + datePart + "-" + uuidPart;
    }

    private SalesOrderDto mapToDto(SalesOrder so) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setId(so.getId());
        dto.setSoNumber(so.getSoNumber());
        dto.setCustomerId(so.getCustomer().getId());
        dto.setCustomerName(so.getCustomer().getName());
        if (so.getWarehouse() != null) {
            dto.setWarehouseId(so.getWarehouse().getId());
            dto.setWarehouseName(so.getWarehouse().getName());
        }
        dto.setOrderDate(so.getOrderDate());
        dto.setExpectedDeliveryDate(so.getExpectedDeliveryDate());
        dto.setStatus(so.getStatus());
        dto.setPriority(so.getPriority());
        dto.setSubtotalAmount(so.getSubtotalAmount());
        dto.setDiscountAmount(so.getDiscountAmount());
        dto.setTotalAmount(so.getTotalAmount());
        dto.setSalesChannel(so.getSalesChannel());
        dto.setAppliedCouponCodes(so.getAppliedCouponCodes());
        dto.setCurrency(so.getCurrency());
        dto.setNotes(so.getNotes());
        dto.setCreatedAt(so.getCreatedAt());
        dto.setUpdatedAt(so.getUpdatedAt());
        dto.setCreatedBy(so.getCreatedBy());
        dto.setUpdatedBy(so.getUpdatedBy());

        List<SalesOrderItemDto> itemDtos = so.getItems().stream().map(item -> {
            SalesOrderItemDto itemDto = new SalesOrderItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductVariantId(item.getProductVariant().getId());
            itemDto.setProductVariantName(item.getProductVariant().getSku());
            itemDto.setSku(item.getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setBaseUnitPrice(item.getBaseUnitPrice());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setLineDiscount(item.getLineDiscount());
            itemDto.setAppliedPromotionCodes(item.getAppliedPromotionCodes());
            itemDto.setTotalPrice(item.getTotalPrice());
            itemDto.setShippedQuantity(item.getShippedQuantity());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }

    private Map<UUID, Deque<PricingEvaluationLine>> buildLineQueues(PricingEvaluation evaluation) {
        Map<UUID, Deque<PricingEvaluationLine>> lines = new HashMap<>();
        for (PricingEvaluationLine line : evaluation.getLines()) {
            lines.computeIfAbsent(line.getProductVariant().getId(), ignored -> new ArrayDeque<>()).add(line);
        }
        return lines;
    }

    private PricingEvaluationLine popPricedLine(Map<UUID, Deque<PricingEvaluationLine>> linesByVariant, UUID productVariantId) {
        Deque<PricingEvaluationLine> lines = linesByVariant.get(productVariantId);
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("Unable to resolve pricing line for product variant: " + productVariantId);
        }
        return lines.removeFirst();
    }
}
