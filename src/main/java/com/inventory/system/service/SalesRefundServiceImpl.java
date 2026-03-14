package com.inventory.system.service;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.CustomerStoreCreditTransaction;
import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageReasonCode;
import com.inventory.system.common.entity.DamageRecordSourceType;
import com.inventory.system.common.entity.PosPaymentMethod;
import com.inventory.system.common.entity.PosRefundSettlementImpact;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosShift;
import com.inventory.system.common.entity.PosShiftStatus;
import com.inventory.system.common.entity.PosTerminal;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.RefundMethod;
import com.inventory.system.common.entity.ReturnDisposition;
import com.inventory.system.common.entity.ReturnMerchandiseAuthorization;
import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.SalesRefund;
import com.inventory.system.common.entity.SalesRefundAuditAction;
import com.inventory.system.common.entity.SalesRefundAuditEntry;
import com.inventory.system.common.entity.SalesRefundItem;
import com.inventory.system.common.entity.SalesRefundStatus;
import com.inventory.system.common.entity.SalesRefundType;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.StoreCreditTransactionType;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateDamageRecordRequest;
import com.inventory.system.payload.CreateExchangeReplacementItemRequest;
import com.inventory.system.payload.CreateSalesRefundItemRequest;
import com.inventory.system.payload.CreateSalesRefundRequest;
import com.inventory.system.payload.RefundDocumentDto;
import com.inventory.system.payload.RefundStatusDecisionRequest;
import com.inventory.system.payload.SalesRefundAuditEntryDto;
import com.inventory.system.payload.SalesRefundDto;
import com.inventory.system.payload.SalesRefundItemDto;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.CustomerStoreCreditTransactionRepository;
import com.inventory.system.repository.PosRefundSettlementImpactRepository;
import com.inventory.system.repository.PosSaleRepository;
import com.inventory.system.repository.PosShiftRepository;
import com.inventory.system.repository.PosTerminalRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.ReturnMerchandiseAuthorizationRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.SalesRefundItemRepository;
import com.inventory.system.repository.SalesRefundRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesRefundServiceImpl implements SalesRefundService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Set<SalesRefundStatus> RESERVED_REFUND_STATUSES = EnumSet.of(
            SalesRefundStatus.PENDING_APPROVAL,
            SalesRefundStatus.APPROVED,
            SalesRefundStatus.COMPLETED
    );

    private final SalesRefundRepository salesRefundRepository;
    private final SalesRefundItemRepository salesRefundItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ReturnMerchandiseAuthorizationRepository rmaRepository;
    private final PosSaleRepository posSaleRepository;
    private final PosShiftRepository posShiftRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final PosRefundSettlementImpactRepository posRefundSettlementImpactRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final CustomerStoreCreditTransactionRepository customerStoreCreditTransactionRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final BatchRepository batchRepository;
    private final StockService stockService;
    private final DamageRecordService damageRecordService;

    @Override
    @Transactional
    public SalesRefundDto createRefund(CreateSalesRefundRequest request) {
        SalesOrder salesOrder = getSalesOrder(request.getSalesOrderId());
        validateRefundableSalesOrder(salesOrder);

        Warehouse warehouse = resolveWarehouse(request.getWarehouseId(), salesOrder);
        ReturnMerchandiseAuthorization rma = resolveRma(request.getRmaId(), salesOrder);
        PosSale posSale = posSaleRepository.findFirstBySalesOrderIdOrderBySaleTimeDesc(salesOrder.getId()).orElse(null);

        SalesRefund refund = new SalesRefund();
        refund.setRefundNumber(generateRefundNumber());
        refund.setSalesOrder(salesOrder);
        refund.setCustomer(salesOrder.getCustomer());
        refund.setWarehouse(warehouse);
        refund.setRma(rma);
        refund.setPosSale(posSale);
        refund.setRefundMethod(request.getRefundMethod());
        refund.setOriginalPaymentMethod(posSale != null ? posSale.getPaymentMethod() : null);
        refund.setRequestedAt(LocalDateTime.now());
        refund.setReason(trimToNull(request.getReason()));
        refund.setNotes(trimToNull(request.getNotes()));

        validateRefundMethod(refund.getRefundMethod(), posSale);

        Map<UUID, SalesOrderItem> orderItemMap = salesOrder.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));
        Map<UUID, Batch> batchMap = loadBatchMap(request.getItems());
        Map<UUID, StorageLocation> locationMap = loadLocationMap(request.getItems(), warehouse);

        BigDecimal subtotalAmount = ZERO;
        List<SalesRefundItem> refundItems = new ArrayList<>();
        for (CreateSalesRefundItemRequest itemRequest : request.getItems()) {
            SalesOrderItem orderItem = orderItemMap.get(itemRequest.getSalesOrderItemId());
            if (orderItem == null) {
                throw new BadRequestException("Refund item does not belong to the selected sales order: " + itemRequest.getSalesOrderItemId());
            }

            BigDecimal remainingRefundableQuantity = calculateRemainingRefundableQuantity(orderItem, rma);
            if (itemRequest.getQuantity().compareTo(remainingRefundableQuantity) > 0) {
                throw new BadRequestException(
                        "Refund quantity exceeds remaining refundable quantity for sales order item "
                                + orderItem.getId() + ". Remaining: " + remainingRefundableQuantity
                );
            }

            validateSerialNumbers(orderItem, itemRequest.getQuantity(), itemRequest.getSerialNumbers());

            BigDecimal unitPrice = itemRequest.getUnitPrice() != null ? itemRequest.getUnitPrice() : orderItem.getUnitPrice();
            BigDecimal refundAmount = unitPrice.multiply(itemRequest.getQuantity());

            SalesRefundItem refundItem = new SalesRefundItem();
            refundItem.setSalesRefund(refund);
            refundItem.setSalesOrderItem(orderItem);
            refundItem.setProductVariant(orderItem.getProductVariant());
            refundItem.setBatch(itemRequest.getBatchId() == null ? null : batchMap.get(itemRequest.getBatchId()));
            refundItem.setStorageLocation(itemRequest.getStorageLocationId() == null ? null : locationMap.get(itemRequest.getStorageLocationId()));
            refundItem.setQuantity(itemRequest.getQuantity());
            refundItem.setUnitPrice(unitPrice);
            refundItem.setRefundAmount(refundAmount);
            refundItem.setReturnDisposition(itemRequest.getReturnDisposition());
            refundItem.setReason(trimToNull(itemRequest.getReason()));
            refundItem.setSerialNumbers(joinSerialNumbers(itemRequest.getSerialNumbers()));
            refundItems.add(refundItem);
            subtotalAmount = subtotalAmount.add(refundAmount);
        }

        refund.setItems(refundItems);
        refund.setSubtotalAmount(subtotalAmount);
        refund.setStatus(SalesRefundStatus.PENDING_APPROVAL);

        BigDecimal replacementAmount = ZERO;
        SalesOrder replacementOrder = null;
        List<CreateExchangeReplacementItemRequest> replacementItems = request.getReplacementItems() == null
                ? List.of()
                : request.getReplacementItems();
        SalesRefundType refundType = resolveRefundType(request, replacementItems);
        if (refundType == SalesRefundType.EXCHANGE) {
            replacementOrder = createReplacementOrder(salesOrder, warehouse, refund.getRefundNumber(), replacementItems);
            replacementAmount = replacementOrder.getTotalAmount();
            refund.setReplacementSalesOrder(replacementOrder);
        }

        refund.setRefundType(refundType);
        refund.setReplacementAmount(replacementAmount);
        refund.setExchangePriceDifference(replacementAmount.subtract(subtotalAmount));
        refund.setNetRefundAmount(subtotalAmount.subtract(replacementAmount).max(ZERO));
        refund.setAmountDueFromCustomer(replacementAmount.subtract(subtotalAmount).max(ZERO));

        SalesRefund savedRefund = salesRefundRepository.save(refund);
        addAuditEntry(savedRefund, SalesRefundAuditAction.CREATED, null, SalesRefundStatus.PENDING_APPROVAL, savedRefund.getNotes());
        if (replacementOrder != null) {
            addAuditEntry(savedRefund, SalesRefundAuditAction.EXCHANGE_ORDER_CREATED, null, SalesRefundStatus.PENDING_APPROVAL,
                    "Replacement order created: " + replacementOrder.getSoNumber());
        }
        return mapToDto(salesRefundRepository.save(savedRefund));
    }

    @Override
    @Transactional(readOnly = true)
    public SalesRefundDto getRefund(UUID id) {
        return mapToDto(getRefundEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesRefundDto> getRefunds(UUID salesOrderId, UUID customerId, SalesRefundStatus status, String refundNumber,
                                           int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Specification<SalesRefund> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (salesOrderId != null) {
                predicates.add(cb.equal(root.get("salesOrder").get("id"), salesOrderId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (refundNumber != null && !refundNumber.isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("refundNumber")), "%" + refundNumber.trim().toUpperCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return salesRefundRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional
    public SalesRefundDto approveRefund(UUID id, RefundStatusDecisionRequest request) {
        SalesRefund refund = getRefundEntity(id);
        transition(refund, SalesRefundStatus.PENDING_APPROVAL, SalesRefundStatus.APPROVED, SalesRefundAuditAction.APPROVED,
                request == null ? null : request.getNotes());
        refund.setApprovedAt(LocalDateTime.now());
        return mapToDto(salesRefundRepository.save(refund));
    }

    @Override
    @Transactional
    public SalesRefundDto rejectRefund(UUID id, RefundStatusDecisionRequest request) {
        SalesRefund refund = getRefundEntity(id);
        transition(refund, SalesRefundStatus.PENDING_APPROVAL, SalesRefundStatus.REJECTED, SalesRefundAuditAction.REJECTED,
                request == null ? null : request.getNotes());
        refund.setRejectedAt(LocalDateTime.now());
        return mapToDto(salesRefundRepository.save(refund));
    }

    @Override
    @Transactional
    public SalesRefundDto completeRefund(UUID id, RefundStatusDecisionRequest request) {
        SalesRefund refund = getRefundEntity(id);
        if (refund.getStatus() != SalesRefundStatus.APPROVED) {
            throw new BadRequestException("Only approved refunds can be completed");
        }

        processReturnDispositions(refund);

        BigDecimal storeCreditIssued = ZERO;
        if (refund.getRefundMethod() == RefundMethod.STORE_CREDIT && refund.getNetRefundAmount().compareTo(ZERO) > 0) {
            storeCreditIssued = issueStoreCredit(refund.getCustomer(), refund, refund.getNetRefundAmount(), request == null ? null : request.getNotes());
            addAuditEntry(refund, SalesRefundAuditAction.STORE_CREDIT_ISSUED, refund.getStatus(), refund.getStatus(),
                    "Store credit issued: " + storeCreditIssued);
        }

        if (refund.getCreditNoteNumber() == null || refund.getDocumentContent() == null) {
            generateCreditNoteInternal(refund);
        }

        refund.setStoreCreditIssued(storeCreditIssued);
        refund.setStatus(SalesRefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        recordSettlementImpact(refund, request);
        addAuditEntry(refund, SalesRefundAuditAction.COMPLETED, SalesRefundStatus.APPROVED, SalesRefundStatus.COMPLETED,
                request == null ? null : request.getNotes());

        return mapToDto(salesRefundRepository.save(refund));
    }

    @Override
    @Transactional
    public SalesRefundDto cancelRefund(UUID id, RefundStatusDecisionRequest request) {
        SalesRefund refund = getRefundEntity(id);
        if (refund.getStatus() == SalesRefundStatus.COMPLETED) {
            throw new BadRequestException("Completed refunds cannot be cancelled");
        }
        if (refund.getStatus() == SalesRefundStatus.REJECTED) {
            throw new BadRequestException("Rejected refunds cannot be cancelled");
        }

        SalesRefundStatus previous = refund.getStatus();
        refund.setStatus(SalesRefundStatus.CANCELLED);
        refund.setCancelledAt(LocalDateTime.now());
        addAuditEntry(refund, SalesRefundAuditAction.CANCELLED, previous, SalesRefundStatus.CANCELLED,
                request == null ? null : request.getNotes());
        return mapToDto(salesRefundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundDocumentDto generateCreditNote(UUID id) {
        SalesRefund refund = getRefundEntity(id);
        return mapDocumentDto(salesRefundRepository.save(generateCreditNoteInternal(refund)));
    }

    private SalesRefund generateCreditNoteInternal(SalesRefund refund) {
        if (refund.getCreditNoteNumber() == null) {
            refund.setCreditNoteNumber(generateCreditNoteNumber());
        }
        refund.setDocumentGeneratedAt(LocalDateTime.now());
        refund.setDocumentContent(buildDocumentContent(refund));
        addAuditEntry(refund, SalesRefundAuditAction.CREDIT_NOTE_GENERATED, refund.getStatus(), refund.getStatus(),
                "Credit note generated: " + refund.getCreditNoteNumber());
        return refund;
    }

    private void processReturnDispositions(SalesRefund refund) {
        List<CreateDamageRecordRequest.ItemRequest> damageItems = new ArrayList<>();

        for (SalesRefundItem item : refund.getItems()) {
            if (item.getReturnDisposition() == ReturnDisposition.RETURN_TO_STOCK) {
                restockItem(refund, item);
                continue;
            }

            CreateDamageRecordRequest.ItemRequest damageItem = new CreateDamageRecordRequest.ItemRequest();
            damageItem.setProductVariantId(item.getProductVariant().getId());
            damageItem.setBatchId(item.getBatch() == null ? null : item.getBatch().getId());
            damageItem.setSourceStorageLocationId(item.getStorageLocation() == null ? null : item.getStorageLocation().getId());
            damageItem.setQuantity(item.getQuantity());
            damageItem.setDisposition(item.getReturnDisposition() == ReturnDisposition.SCRAP
                    ? DamageDispositionType.WRITE_OFF
                    : DamageDispositionType.QUARANTINE);
            damageItem.setSerialNumbers(splitSerialNumbers(item.getSerialNumbers()));
            damageItems.add(damageItem);
        }

        if (!damageItems.isEmpty()) {
            CreateDamageRecordRequest damageRequest = new CreateDamageRecordRequest();
            damageRequest.setWarehouseId(refund.getWarehouse().getId());
            damageRequest.setSourceType(DamageRecordSourceType.SALES_RETURN);
            damageRequest.setReasonCode(DamageReasonCode.DAMAGED);
            damageRequest.setReference(refund.getRefundNumber());
            damageRequest.setNotes(buildDamageNotes(refund));
            damageRequest.setItems(damageItems);

            var damageRecord = damageRecordService.createDamageRecord(damageRequest);
            damageRecordService.submitForApproval(damageRecord.getId());
            damageRecordService.approveDamageRecord(damageRecord.getId());
            damageRecordService.confirmDamageRecord(damageRecord.getId());
        }
    }

    private void restockItem(SalesRefund refund, SalesRefundItem item) {
        StockAdjustmentDto adjustment = new StockAdjustmentDto();
        adjustment.setProductVariantId(item.getProductVariant().getId());
        adjustment.setWarehouseId(refund.getWarehouse().getId());
        adjustment.setStorageLocationId(item.getStorageLocation() == null ? null : item.getStorageLocation().getId());
        adjustment.setBatchId(item.getBatch() == null ? null : item.getBatch().getId());
        adjustment.setQuantity(item.getQuantity());
        adjustment.setType(StockMovement.StockMovementType.IN);
        adjustment.setReason("Sales refund restock " + refund.getRefundNumber());
        adjustment.setReferenceId(refund.getId().toString());
        adjustment.setSerialNumbers(splitSerialNumbers(item.getSerialNumbers()));
        stockService.adjustStock(adjustment);
    }

    private BigDecimal issueStoreCredit(Customer customer, SalesRefund refund, BigDecimal amount, String notes) {
        BigDecimal before = customer.getStoreCreditBalance() != null ? customer.getStoreCreditBalance() : ZERO;
        BigDecimal after = before.add(amount);
        customer.setStoreCreditBalance(after);
        customerRepository.save(customer);

        CustomerStoreCreditTransaction transaction = new CustomerStoreCreditTransaction();
        transaction.setCustomer(customer);
        transaction.setSalesRefund(refund);
        transaction.setType(StoreCreditTransactionType.ISSUED);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(before);
        transaction.setBalanceAfter(after);
        transaction.setReferenceNumber(refund.getRefundNumber());
        transaction.setNotes(trimToNull(notes));
        transaction.setTransactionDate(LocalDateTime.now());
        customerStoreCreditTransactionRepository.save(transaction);
        return amount;
    }

    private SalesOrder createReplacementOrder(SalesOrder originalOrder,
                                              Warehouse warehouse,
                                              String refundNumber,
                                              List<CreateExchangeReplacementItemRequest> replacementItems) {
        if (replacementItems == null || replacementItems.isEmpty()) {
            throw new BadRequestException("Exchange refunds require replacement items");
        }

        Set<UUID> variantIds = replacementItems.stream()
                .map(CreateExchangeReplacementItemRequest::getProductVariantId)
                .collect(Collectors.toSet());
        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("One or more replacement items reference an unknown product variant");
        }

        SalesOrder replacementOrder = new SalesOrder();
        replacementOrder.setSoNumber(generateReplacementSoNumber());
        replacementOrder.setCustomer(originalOrder.getCustomer());
        replacementOrder.setWarehouse(warehouse);
        replacementOrder.setOrderDate(LocalDateTime.now());
        replacementOrder.setExpectedDeliveryDate(originalOrder.getExpectedDeliveryDate());
        replacementOrder.setStatus(SalesOrderStatus.DRAFT);
        replacementOrder.setPriority(originalOrder.getPriority());
        replacementOrder.setCurrency(originalOrder.getCurrency());
        replacementOrder.setNotes("Exchange replacement created from refund " + refundNumber);

        BigDecimal total = ZERO;
        List<SalesOrderItem> replacementOrderItems = new ArrayList<>();
        for (CreateExchangeReplacementItemRequest itemRequest : replacementItems) {
            ProductVariant variant = variantMap.get(itemRequest.getProductVariantId());
            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setSalesOrder(replacementOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            orderItem.setTotalPrice(itemRequest.getUnitPrice().multiply(itemRequest.getQuantity()));
            orderItem.setShippedQuantity(ZERO);
            replacementOrderItems.add(orderItem);
            total = total.add(orderItem.getTotalPrice());
        }

        replacementOrder.setItems(replacementOrderItems);
        replacementOrder.setTotalAmount(total);
        return salesOrderRepository.save(replacementOrder);
    }

    private SalesRefundType resolveRefundType(CreateSalesRefundRequest request, List<CreateExchangeReplacementItemRequest> replacementItems) {
        SalesRefundType resolved = request.getRefundType();
        if (resolved == null) {
            if (replacementItems != null && !replacementItems.isEmpty()) {
                resolved = SalesRefundType.EXCHANGE;
            } else if (request.getRefundMethod() == RefundMethod.STORE_CREDIT) {
                resolved = SalesRefundType.STORE_CREDIT;
            } else {
                resolved = SalesRefundType.REFUND;
            }
        }

        if (resolved == SalesRefundType.EXCHANGE && (replacementItems == null || replacementItems.isEmpty())) {
            throw new BadRequestException("Refund type EXCHANGE requires replacement items");
        }
        if (resolved == SalesRefundType.STORE_CREDIT && request.getRefundMethod() != RefundMethod.STORE_CREDIT) {
            throw new BadRequestException("Store credit refunds must use refund method STORE_CREDIT");
        }
        return resolved;
    }

    private void validateRefundableSalesOrder(SalesOrder salesOrder) {
        if (salesOrder.getStatus() != SalesOrderStatus.SHIPPED
                && salesOrder.getStatus() != SalesOrderStatus.DELIVERED
                && salesOrder.getStatus() != SalesOrderStatus.RETURNED) {
            throw new BadRequestException("Refunds can only be created for shipped, delivered, or returned sales orders");
        }
    }

    private void validateRefundMethod(RefundMethod refundMethod, PosSale posSale) {
        if (refundMethod != RefundMethod.ORIGINAL_PAYMENT_METHOD) {
            return;
        }
        if (posSale == null) {
            throw new BadRequestException("Refund to original payment method is only available for POS-linked sales");
        }
        if (posSale.getPaymentMethod() == PosPaymentMethod.MIXED || posSale.getPaymentMethod() == PosPaymentMethod.OTHER) {
            throw new BadRequestException("Original payment refund is not supported for mixed or unsupported payment methods");
        }
    }

    private void recordSettlementImpact(SalesRefund refund, RefundStatusDecisionRequest request) {
        if (refund.getNetRefundAmount() == null || refund.getNetRefundAmount().compareTo(ZERO) <= 0) {
            return;
        }

        PosPaymentMethod settlementMethod = resolveSettlementPaymentMethod(refund);
        if (settlementMethod == null) {
            return;
        }

        PosShift shift = resolveSettlementShift(refund, request);
        PosTerminal terminal = resolveSettlementTerminal(refund, request, shift);
        if (settlementMethod == PosPaymentMethod.CASH && shift == null) {
            throw new BadRequestException("Cash refunds require an active or explicit POS shift for settlement tracking");
        }

        PosRefundSettlementImpact impact = new PosRefundSettlementImpact();
        impact.setSalesRefund(refund);
        impact.setShift(shift);
        impact.setTerminal(terminal);
        impact.setPaymentMethod(settlementMethod);
        impact.setAmount(refund.getNetRefundAmount());
        impact.setOccurredAt(LocalDateTime.now());
        impact.setReferenceNumber(refund.getRefundNumber());
        impact.setNotes(request == null ? null : request.getNotes());
        posRefundSettlementImpactRepository.save(impact);
    }

    private PosPaymentMethod resolveSettlementPaymentMethod(SalesRefund refund) {
        RefundMethod refundMethod = refund.getRefundMethod();
        if (refundMethod == RefundMethod.STORE_CREDIT) {
            return null;
        }
        if (refundMethod == RefundMethod.ORIGINAL_PAYMENT_METHOD) {
            return refund.getOriginalPaymentMethod();
        }
        return switch (refundMethod) {
            case CASH -> PosPaymentMethod.CASH;
            case CARD -> PosPaymentMethod.CARD;
            case TRANSFER -> PosPaymentMethod.TRANSFER;
            case OTHER -> PosPaymentMethod.OTHER;
            default -> null;
        };
    }

    private PosShift resolveSettlementShift(SalesRefund refund, RefundStatusDecisionRequest request) {
        if (request != null && request.getShiftId() != null) {
            return posShiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + request.getShiftId()));
        }

        PosTerminal terminal = resolveSettlementTerminal(refund, request, null);
        if (terminal == null) {
            return null;
        }
        return posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminal.getId(), PosShiftStatus.OPEN)
                .orElse(null);
    }

    private PosTerminal resolveSettlementTerminal(SalesRefund refund, RefundStatusDecisionRequest request, PosShift shift) {
        if (shift != null) {
            return shift.getTerminal();
        }
        if (request != null && request.getTerminalId() != null) {
            return posTerminalRepository.findById(request.getTerminalId())
                    .orElseThrow(() -> new ResourceNotFoundException("POS terminal not found with ID: " + request.getTerminalId()));
        }
        return refund.getPosSale() == null ? null : refund.getPosSale().getTerminal();
    }

    private Warehouse resolveWarehouse(UUID warehouseId, SalesOrder salesOrder) {
        UUID targetWarehouseId = warehouseId != null ? warehouseId : salesOrder.getWarehouse().getId();
        return warehouseRepository.findById(targetWarehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", targetWarehouseId));
    }

    private ReturnMerchandiseAuthorization resolveRma(UUID rmaId, SalesOrder salesOrder) {
        if (rmaId == null) {
            return null;
        }
        ReturnMerchandiseAuthorization rma = rmaRepository.findById(rmaId)
                .orElseThrow(() -> new ResourceNotFoundException("RMA", "id", rmaId));
        if (!rma.getSalesOrder().getId().equals(salesOrder.getId())) {
            throw new BadRequestException("RMA does not belong to the provided sales order");
        }
        if (rma.getStatus() != ReturnMerchandiseStatus.RECEIVED && rma.getStatus() != ReturnMerchandiseStatus.COMPLETED) {
            throw new BadRequestException("Refunds linked to an RMA require the RMA to be received or completed");
        }
        return rma;
    }

    private BigDecimal calculateRemainingRefundableQuantity(SalesOrderItem orderItem, ReturnMerchandiseAuthorization rma) {
        BigDecimal refundedQuantity = salesRefundItemRepository.sumQuantityBySalesOrderItemIdAndRefundStatusIn(
                orderItem.getId(), RESERVED_REFUND_STATUSES
        );

        BigDecimal eligibleQuantity = orderItem.getShippedQuantity() != null ? orderItem.getShippedQuantity() : ZERO;
        if (rma != null) {
            eligibleQuantity = rma.getItems().stream()
                    .filter(item -> item.getSalesOrderItem().getId().equals(orderItem.getId()))
                    .map(item -> item.getQuantity() == null ? ZERO : item.getQuantity())
                    .reduce(ZERO, BigDecimal::add);
        }

        BigDecimal remaining = eligibleQuantity.subtract(refundedQuantity);
        return remaining.max(ZERO);
    }

    private Map<UUID, Batch> loadBatchMap(List<CreateSalesRefundItemRequest> items) {
        Set<UUID> batchIds = items.stream()
                .map(CreateSalesRefundItemRequest::getBatchId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        return batchRepository.findAllById(batchIds).stream()
                .collect(Collectors.toMap(Batch::getId, Function.identity()));
    }

    private Map<UUID, StorageLocation> loadLocationMap(List<CreateSalesRefundItemRequest> items, Warehouse warehouse) {
        Set<UUID> locationIds = items.stream()
                .map(CreateSalesRefundItemRequest::getStorageLocationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (locationIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, StorageLocation> locationMap = storageLocationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(StorageLocation::getId, Function.identity()));
        if (locationMap.size() != locationIds.size()) {
            throw new BadRequestException("One or more storage locations were not found");
        }
        for (StorageLocation location : locationMap.values()) {
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new BadRequestException("Storage location does not belong to the selected warehouse");
            }
        }
        return locationMap;
    }

    private void validateSerialNumbers(SalesOrderItem orderItem, BigDecimal quantity, List<String> serialNumbers) {
        Boolean serialTracked = orderItem.getProductVariant().getTemplate().getIsSerialTracked();
        if (!Boolean.TRUE.equals(serialTracked)) {
            return;
        }
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            throw new BadRequestException("Serial numbers are required for serial-tracked refund items");
        }
        if (quantity.remainder(BigDecimal.ONE).compareTo(ZERO) != 0) {
            throw new BadRequestException("Serial-tracked refund quantities must be whole numbers");
        }
        if (BigDecimal.valueOf(serialNumbers.size()).compareTo(quantity) != 0) {
            throw new BadRequestException("Serial number count must match refund quantity");
        }
    }

    private SalesRefund getRefundEntity(UUID id) {
        return salesRefundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesRefund", "id", id));
    }

    private SalesOrder getSalesOrder(UUID id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
    }

    private void transition(SalesRefund refund, SalesRefundStatus from, SalesRefundStatus to,
                            SalesRefundAuditAction action, String notes) {
        if (refund.getStatus() != from) {
            throw new BadRequestException("Refund must be in status " + from + " to transition to " + to);
        }
        refund.setStatus(to);
        addAuditEntry(refund, action, from, to, trimToNull(notes));
    }

    private void addAuditEntry(SalesRefund refund, SalesRefundAuditAction action, SalesRefundStatus from, SalesRefundStatus to, String notes) {
        SalesRefundAuditEntry entry = new SalesRefundAuditEntry();
        entry.setSalesRefund(refund);
        entry.setAction(action);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setNotes(trimToNull(notes));
        entry.setActedAt(LocalDateTime.now());
        refund.getAuditEntries().add(entry);
    }

    private RefundDocumentDto mapDocumentDto(SalesRefund refund) {
        RefundDocumentDto dto = new RefundDocumentDto();
        dto.setSalesRefundId(refund.getId());
        dto.setRefundNumber(refund.getRefundNumber());
        dto.setCreditNoteNumber(refund.getCreditNoteNumber());
        dto.setDocumentContent(refund.getDocumentContent());
        dto.setGeneratedAt(refund.getDocumentGeneratedAt());
        return dto;
    }

    private SalesRefundDto mapToDto(SalesRefund refund) {
        SalesRefundDto dto = new SalesRefundDto();
        dto.setId(refund.getId());
        dto.setRefundNumber(refund.getRefundNumber());
        dto.setCreditNoteNumber(refund.getCreditNoteNumber());
        dto.setSalesOrderId(refund.getSalesOrder().getId());
        dto.setSoNumber(refund.getSalesOrder().getSoNumber());
        dto.setCustomerId(refund.getCustomer().getId());
        dto.setCustomerName(refund.getCustomer().getName());
        dto.setWarehouseId(refund.getWarehouse().getId());
        dto.setWarehouseName(refund.getWarehouse().getName());
        if (refund.getRma() != null) {
            dto.setRmaId(refund.getRma().getId());
            dto.setRmaNumber(refund.getRma().getRmaNumber());
        }
        if (refund.getPosSale() != null) {
            dto.setPosSaleId(refund.getPosSale().getId());
            dto.setReceiptNumber(refund.getPosSale().getReceiptNumber());
        }
        if (refund.getReplacementSalesOrder() != null) {
            dto.setReplacementSalesOrderId(refund.getReplacementSalesOrder().getId());
            dto.setReplacementSalesOrderNumber(refund.getReplacementSalesOrder().getSoNumber());
        }
        dto.setStatus(refund.getStatus());
        dto.setRefundType(refund.getRefundType());
        dto.setRefundMethod(refund.getRefundMethod());
        dto.setOriginalPaymentMethod(refund.getOriginalPaymentMethod());
        dto.setRequestedAt(refund.getRequestedAt());
        dto.setApprovedAt(refund.getApprovedAt());
        dto.setCompletedAt(refund.getCompletedAt());
        dto.setRejectedAt(refund.getRejectedAt());
        dto.setCancelledAt(refund.getCancelledAt());
        dto.setReason(refund.getReason());
        dto.setNotes(refund.getNotes());
        dto.setSubtotalAmount(refund.getSubtotalAmount());
        dto.setReplacementAmount(refund.getReplacementAmount());
        dto.setNetRefundAmount(refund.getNetRefundAmount());
        dto.setAmountDueFromCustomer(refund.getAmountDueFromCustomer());
        dto.setStoreCreditIssued(refund.getStoreCreditIssued());
        dto.setExchangePriceDifference(refund.getExchangePriceDifference());
        dto.setDocumentGeneratedAt(refund.getDocumentGeneratedAt());
        dto.setItems(refund.getItems().stream().map(this::mapItemToDto).toList());
        dto.setAuditEntries(refund.getAuditEntries().stream()
                .sorted(Comparator.comparing(SalesRefundAuditEntry::getActedAt).reversed())
                .map(this::mapAuditToDto)
                .toList());
        dto.setCreatedAt(refund.getCreatedAt());
        dto.setUpdatedAt(refund.getUpdatedAt());
        dto.setCreatedBy(refund.getCreatedBy());
        dto.setUpdatedBy(refund.getUpdatedBy());
        return dto;
    }

    private SalesRefundItemDto mapItemToDto(SalesRefundItem item) {
        SalesRefundItemDto dto = new SalesRefundItemDto();
        dto.setId(item.getId());
        dto.setSalesOrderItemId(item.getSalesOrderItem().getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setRefundAmount(item.getRefundAmount());
        dto.setReturnDisposition(item.getReturnDisposition());
        dto.setReason(item.getReason());
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
            dto.setBatchNumber(item.getBatch().getBatchNumber());
        }
        if (item.getStorageLocation() != null) {
            dto.setStorageLocationId(item.getStorageLocation().getId());
            dto.setStorageLocationName(item.getStorageLocation().getName());
        }
        dto.setSerialNumbers(splitSerialNumbers(item.getSerialNumbers()));
        return dto;
    }

    private SalesRefundAuditEntryDto mapAuditToDto(SalesRefundAuditEntry entry) {
        SalesRefundAuditEntryDto dto = new SalesRefundAuditEntryDto();
        dto.setId(entry.getId());
        dto.setAction(entry.getAction());
        dto.setFromStatus(entry.getFromStatus());
        dto.setToStatus(entry.getToStatus());
        dto.setNotes(entry.getNotes());
        dto.setActedAt(entry.getActedAt());
        dto.setCreatedBy(entry.getCreatedBy());
        return dto;
    }

    private String generateRefundNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        for (int sequence = 1; sequence < 1000; sequence++) {
            String candidate = "RF-" + datePart + "-" + String.format("%03d", sequence);
            if (!salesRefundRepository.existsByRefundNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate refund number");
    }

    private String generateCreditNoteNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        for (int sequence = 1; sequence < 1000; sequence++) {
            String candidate = "CN-" + datePart + "-" + String.format("%03d", sequence);
            if (!salesRefundRepository.existsByCreditNoteNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate credit note number");
    }

    private String generateReplacementSoNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        for (int sequence = 1; sequence < 1000; sequence++) {
            String candidate = "SOX-" + datePart + "-" + String.format("%03d", sequence);
            if (!salesOrderRepository.existsBySoNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate replacement sales order number");
    }

    private String buildDocumentContent(SalesRefund refund) {
        StringBuilder builder = new StringBuilder();
        builder.append("Credit Note: ").append(refund.getCreditNoteNumber()).append("\n");
        builder.append("Refund Number: ").append(refund.getRefundNumber()).append("\n");
        builder.append("Sales Order: ").append(refund.getSalesOrder().getSoNumber()).append("\n");
        builder.append("Customer: ").append(refund.getCustomer().getName()).append("\n");
        builder.append("Refund Method: ").append(refund.getRefundMethod()).append("\n");
        builder.append("Refund Type: ").append(refund.getRefundType()).append("\n");
        builder.append("Status: ").append(refund.getStatus()).append("\n");
        builder.append("Requested At: ").append(refund.getRequestedAt()).append("\n\n");
        builder.append("Lines:\n");
        for (SalesRefundItem item : refund.getItems()) {
            builder.append("- ")
                    .append(item.getProductVariant().getSku())
                    .append(" | qty ")
                    .append(item.getQuantity())
                    .append(" | unit ")
                    .append(item.getUnitPrice())
                    .append(" | amount ")
                    .append(item.getRefundAmount())
                    .append(" | disposition ")
                    .append(item.getReturnDisposition())
                    .append("\n");
        }
        builder.append("\nSubtotal Refund: ").append(refund.getSubtotalAmount()).append("\n");
        builder.append("Replacement Amount: ").append(refund.getReplacementAmount()).append("\n");
        builder.append("Net Refund Amount: ").append(refund.getNetRefundAmount()).append("\n");
        builder.append("Amount Due From Customer: ").append(refund.getAmountDueFromCustomer()).append("\n");
        builder.append("Store Credit Issued: ").append(refund.getStoreCreditIssued()).append("\n");
        if (refund.getReason() != null) {
            builder.append("Reason: ").append(refund.getReason()).append("\n");
        }
        if (refund.getNotes() != null) {
            builder.append("Notes: ").append(refund.getNotes()).append("\n");
        }
        return builder.toString();
    }

    private String buildDamageNotes(SalesRefund refund) {
        if (refund.getNotes() == null || refund.getNotes().isBlank()) {
            return "Generated from refund " + refund.getRefundNumber();
        }
        return "Generated from refund " + refund.getRefundNumber() + ". " + refund.getNotes();
    }

    private String joinSerialNumbers(Collection<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            return null;
        }
        return serialNumbers.stream().map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.joining(","));
    }

    private List<String> splitSerialNumbers(String serialNumbers) {
        if (serialNumbers == null || serialNumbers.isBlank()) {
            return null;
        }
        return java.util.Arrays.stream(serialNumbers.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}