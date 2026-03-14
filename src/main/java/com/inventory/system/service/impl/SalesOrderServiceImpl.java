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

        if (salesOrder.getStatus() != SalesOrderStatus.DRAFT && salesOrder.getStatus() != SalesOrderStatus.PENDING_APPROVAL) {
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

        if (status == SalesOrderStatus.CONFIRMED && previousStatus != SalesOrderStatus.CONFIRMED) {
            validateCustomerEligibility(salesOrder.getCustomer());
            validateCustomerCredit(salesOrder);

            if (previousStatus == SalesOrderStatus.BACKORDERED) {
                stockReservationService.releaseReservationsByReference(salesOrder.getSoNumber());
            }

            boolean hasBackorder = false;

            for (SalesOrderItem item : salesOrder.getItems()) {
                BigDecimal available = stockReservationService.getAvailableToPromise(item.getProductVariant().getId(), salesOrder.getWarehouse().getId());
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

            salesOrder.setStatus(hasBackorder ? SalesOrderStatus.BACKORDERED : SalesOrderStatus.CONFIRMED);
            SalesOrder savedSo = salesOrderRepository.save(salesOrder);
            return mapToDto(savedSo);
        }

        if ((status == SalesOrderStatus.SHIPPED || status == SalesOrderStatus.DELIVERED)
                && previousStatus != SalesOrderStatus.SHIPPED
                && previousStatus != SalesOrderStatus.DELIVERED) {
            stockReservationService.fulfillReservationsByReference(salesOrder.getSoNumber());
        }

        if (status == SalesOrderStatus.CANCELLED && previousStatus != SalesOrderStatus.CANCELLED) {
            stockReservationService.releaseReservationsByReference(salesOrder.getSoNumber());
        }

        salesOrder.setStatus(status);
        SalesOrder savedSo = salesOrderRepository.save(salesOrder);
        return mapToDto(savedSo);
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

    private void validateStatusTransition(SalesOrderStatus currentStatus, SalesOrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        switch (currentStatus) {
            case DRAFT:
                if (targetStatus == SalesOrderStatus.PENDING_APPROVAL
                        || targetStatus == SalesOrderStatus.APPROVED
                        || targetStatus == SalesOrderStatus.CONFIRMED
                        || targetStatus == SalesOrderStatus.CANCELLED) {
                    return;
                }
                break;
            case PENDING_APPROVAL:
                if (targetStatus == SalesOrderStatus.DRAFT
                        || targetStatus == SalesOrderStatus.APPROVED
                        || targetStatus == SalesOrderStatus.CANCELLED) {
                    return;
                }
                break;
            case APPROVED:
                if (targetStatus == SalesOrderStatus.CONFIRMED || targetStatus == SalesOrderStatus.CANCELLED) {
                    return;
                }
                break;
            case CONFIRMED:
            case BACKORDERED:
                if (targetStatus == SalesOrderStatus.CONFIRMED
                        || targetStatus == SalesOrderStatus.BACKORDERED
                        || targetStatus == SalesOrderStatus.PARTIALLY_SHIPPED
                        || targetStatus == SalesOrderStatus.SHIPPED
                        || targetStatus == SalesOrderStatus.DELIVERED
                        || targetStatus == SalesOrderStatus.CANCELLED) {
                    return;
                }
                break;
            case PARTIALLY_SHIPPED:
                if (targetStatus == SalesOrderStatus.SHIPPED || targetStatus == SalesOrderStatus.DELIVERED) {
                    return;
                }
                break;
            case SHIPPED:
                if (targetStatus == SalesOrderStatus.DELIVERED || targetStatus == SalesOrderStatus.RETURNED) {
                    return;
                }
                break;
            case DELIVERED:
                if (targetStatus == SalesOrderStatus.RETURNED) {
                    return;
                }
                break;
            default:
                break;
        }

        throw new BadRequestException("Invalid sales order status transition from " + currentStatus + " to " + targetStatus);
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
