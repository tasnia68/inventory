package com.inventory.system.service;

import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageRecord;
import com.inventory.system.common.entity.FinancialEvent;
import com.inventory.system.common.entity.FinancialEventType;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosSalePayment;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.SalesRefund;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockTransaction;
import com.inventory.system.common.entity.StockTransactionItem;
import com.inventory.system.common.entity.SubledgerEntry;
import com.inventory.system.common.entity.SubledgerEntryType;
import com.inventory.system.common.entity.SupplierReturn;
import com.inventory.system.common.entity.SupplierReturnItem;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.FinancialEventDto;
import com.inventory.system.payload.SubledgerEntryDto;
import com.inventory.system.repository.FinancialEventRepository;
import com.inventory.system.repository.StockMovementRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialEventServiceImpl implements FinancialEventService {

    private static final String ACCOUNTING_POSTING_ENABLED_KEY = "accounting.posting.enabled";
    private static final String VALUATION_CURRENCY_KEY = "INVENTORY_VALUATION_CURRENCY";
    private static final String DEFAULT_CURRENCY_KEY = "DEFAULT_CURRENCY";

    private static final String ACCOUNT_INVENTORY_ASSET = "INVENTORY_ASSET";
    private static final String ACCOUNT_GRNI_ACCRUAL = "GRNI_ACCRUAL";
    private static final String ACCOUNT_CASH_CLEARING = "CASH_CLEARING";
    private static final String ACCOUNT_SALES_REVENUE = "SALES_REVENUE";
    private static final String ACCOUNT_COGS = "COGS";
    private static final String ACCOUNT_SALES_RETURNS = "SALES_RETURNS";
    private static final String ACCOUNT_STORE_CREDIT_LIABILITY = "STORE_CREDIT_LIABILITY";
    private static final String ACCOUNT_REFUND_CLEARING = "REFUND_CLEARING";
    private static final String ACCOUNT_SUPPLIER_RETURN_CLEARING = "SUPPLIER_RETURN_CLEARING";
    private static final String ACCOUNT_INVENTORY_ADJUSTMENT_GAIN = "INVENTORY_ADJUSTMENT_GAIN";
    private static final String ACCOUNT_INVENTORY_ADJUSTMENT_LOSS = "INVENTORY_ADJUSTMENT_LOSS";
    private static final String ACCOUNT_INVENTORY_WRITE_OFF = "INVENTORY_WRITE_OFF";

    private final FinancialEventRepository financialEventRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TenantSettingService tenantSettingService;

    @Override
    @Transactional
    public FinancialEventDto recordGoodsReceipt(GoodsReceiptNote goodsReceiptNote) {
        return upsertEvent(
                FinancialEventType.GOODS_RECEIPT,
                "GOODS_RECEIPT_NOTE",
                goodsReceiptNote.getId().toString(),
                goodsReceiptNote.getGrnNumber(),
                goodsReceiptNote.getGrnNumber(),
                "Goods receipt confirmed for " + goodsReceiptNote.getGrnNumber(),
                sumGoodsReceiptAmount(goodsReceiptNote.getItems()),
                defaultCurrency(),
                """
                        {"warehouseId":"%s","supplierId":"%s"}
                        """.formatted(goodsReceiptNote.getWarehouse().getId(), goodsReceiptNote.getSupplier().getId()).trim(),
                List.of(
                        line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", sumGoodsReceiptAmount(goodsReceiptNote.getItems()), defaultCurrency(), "Inventory received"),
                        line(SubledgerEntryType.CREDIT, ACCOUNT_GRNI_ACCRUAL, "Goods Received Not Invoiced", sumGoodsReceiptAmount(goodsReceiptNote.getItems()), defaultCurrency(), "Accrual for received goods")
                )
        );
    }

    @Override
    @Transactional
    public FinancialEventDto recordStockTransaction(StockTransaction stockTransaction) {
        List<StockMovement> movements = stockMovementRepository.findByReferenceIdOrderByCreatedAtAsc(stockTransaction.getId().toString());
        List<LineDefinition> lines = new ArrayList<>();
        switch (stockTransaction.getType()) {
            case TRANSFER -> {
                BigDecimal amount = movements.stream()
                        .filter(movement -> movement.getType() == StockMovement.StockMovementType.TRANSFER_OUT)
                        .map(StockMovement::getTotalCost)
                        .filter(java.util.Objects::nonNull)
                        .map(this::scale)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return null;
                }
                lines.add(line(SubledgerEntryType.DEBIT, "INVENTORY_ASSET_DEST", "Inventory Asset - Destination", amount, defaultCurrency(), "Transfer received into destination warehouse"));
                lines.add(line(SubledgerEntryType.CREDIT, "INVENTORY_ASSET_SOURCE", "Inventory Asset - Source", amount, defaultCurrency(), "Transfer issued from source warehouse"));
                return upsertEvent(FinancialEventType.WAREHOUSE_TRANSFER, "STOCK_TRANSACTION", stockTransaction.getId().toString(),
                        stockTransaction.getTransactionNumber(), stockTransaction.getReference(),
                        "Warehouse transfer completed for " + stockTransaction.getTransactionNumber(), amount, defaultCurrency(),
                        buildStockTransactionMetadata(stockTransaction, movements), lines);
            }
            case ADJUSTMENT -> {
                BigDecimal positiveCost = sumMovementCostByQuantitySign(movements, true);
                BigDecimal negativeCost = sumMovementCostByQuantitySign(movements, false);
                BigDecimal amount = positiveCost.add(negativeCost);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return null;
                }
                if (positiveCost.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", positiveCost, defaultCurrency(), "Inventory gain from adjustment"));
                    lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ADJUSTMENT_GAIN, "Inventory Adjustment Gain", positiveCost, defaultCurrency(), "Offset for positive adjustment"));
                }
                if (negativeCost.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_ADJUSTMENT_LOSS, "Inventory Adjustment Loss", negativeCost, defaultCurrency(), "Loss from inventory adjustment"));
                    lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", negativeCost, defaultCurrency(), "Inventory reduction from adjustment"));
                }
                return upsertEvent(FinancialEventType.STOCK_ADJUSTMENT, "STOCK_TRANSACTION", stockTransaction.getId().toString(),
                        stockTransaction.getTransactionNumber(), stockTransaction.getReference(),
                        "Stock adjustment completed for " + stockTransaction.getTransactionNumber(), amount, defaultCurrency(),
                        buildStockTransactionMetadata(stockTransaction, movements), lines);
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    @Transactional
    public FinancialEventDto recordPosSale(PosSale posSale) {
        BigDecimal revenueAmount = scale(posSale.getTotalAmount());
        BigDecimal costAmount = posSale.getStockTransaction() == null ? BigDecimal.ZERO : posSale.getStockTransaction().getItems().stream()
                .map(this::stockTransactionItemCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentAmount = posSale.getPayments().stream()
                .map(PosSalePayment::getAmount)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            paymentAmount = revenueAmount;
        }

        List<LineDefinition> lines = new ArrayList<>();
        lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing", paymentAmount, posSale.getCurrency(), "POS tender captured"));
        lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_SALES_REVENUE, "Sales Revenue", revenueAmount, posSale.getCurrency(), "POS sale revenue"));
        if (costAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_COGS, "Cost of Goods Sold", costAmount, posSale.getCurrency(), "Cost of goods sold for POS sale"));
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", costAmount, posSale.getCurrency(), "Inventory relieved for POS sale"));
        }

        return upsertEvent(FinancialEventType.POS_SALE, "POS_SALE", posSale.getId().toString(), posSale.getReceiptNumber(),
                posSale.getSalesOrder() == null ? posSale.getReceiptNumber() : posSale.getSalesOrder().getSoNumber(),
                "POS sale completed for receipt " + posSale.getReceiptNumber(), revenueAmount, posSale.getCurrency(),
                """
                        {"warehouseId":"%s","terminalId":"%s","paymentMethod":"%s"}
                        """.formatted(posSale.getWarehouse().getId(), posSale.getTerminal().getId(), posSale.getPaymentMethod()).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordSalesRefund(SalesRefund salesRefund) {
        List<StockMovement> restockMovements = stockMovementRepository.findByReferenceIdOrderByCreatedAtAsc(salesRefund.getId().toString());
        BigDecimal restockValue = sumMovementCost(restockMovements);
        BigDecimal refundAmount = scale(salesRefund.getNetRefundAmount());
        BigDecimal storeCreditAmount = scale(salesRefund.getStoreCreditIssued());
        BigDecimal settlementAmount = storeCreditAmount.compareTo(BigDecimal.ZERO) > 0 ? storeCreditAmount : refundAmount;

        if (settlementAmount.compareTo(BigDecimal.ZERO) <= 0 && restockValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        String clearingAccount = storeCreditAmount.compareTo(BigDecimal.ZERO) > 0
                ? ACCOUNT_STORE_CREDIT_LIABILITY
                : ACCOUNT_REFUND_CLEARING;
        String clearingName = storeCreditAmount.compareTo(BigDecimal.ZERO) > 0
                ? "Store Credit Liability"
                : "Refund Clearing";

        List<LineDefinition> lines = new ArrayList<>();
        if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_SALES_RETURNS, "Sales Returns and Allowances", settlementAmount, defaultCurrency(), "Commercial refund recognized"));
            lines.add(line(SubledgerEntryType.CREDIT, clearingAccount, clearingName, settlementAmount, defaultCurrency(), "Refund settlement obligation"));
        }
        if (restockValue.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", restockValue, defaultCurrency(), "Inventory returned to stock"));
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_COGS, "Cost of Goods Sold", restockValue, defaultCurrency(), "COGS reversal on return"));
        }

        return upsertEvent(FinancialEventType.SALES_REFUND, "SALES_REFUND", salesRefund.getId().toString(), salesRefund.getRefundNumber(),
                salesRefund.getCreditNoteNumber(), "Sales refund completed for " + salesRefund.getRefundNumber(),
                settlementAmount.add(restockValue), defaultCurrency(),
                """
                        {"refundMethod":"%s","refundType":"%s","warehouseId":"%s"}
                        """.formatted(salesRefund.getRefundMethod(), salesRefund.getRefundType(), salesRefund.getWarehouse().getId()).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordSupplierReturn(SupplierReturn supplierReturn) {
        BigDecimal amount = supplierReturn.getItems().stream()
                .map(this::supplierReturnCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return upsertEvent(FinancialEventType.SUPPLIER_RETURN, "SUPPLIER_RETURN", supplierReturn.getId().toString(),
                supplierReturn.getReturnNumber(), supplierReturn.getGoodsReceiptNote().getGrnNumber(),
                "Supplier return completed for " + supplierReturn.getReturnNumber(), amount, defaultCurrency(),
                """
                        {"supplierId":"%s","warehouseId":"%s"}
                        """.formatted(supplierReturn.getSupplier().getId(), supplierReturn.getWarehouse().getId()).trim(),
                List.of(
                        line(SubledgerEntryType.DEBIT, ACCOUNT_SUPPLIER_RETURN_CLEARING, "Supplier Return Clearing", amount, defaultCurrency(), "Expected supplier credit for return"),
                        line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", amount, defaultCurrency(), "Inventory issued back to supplier")
                ));
    }

    @Override
    @Transactional
    public FinancialEventDto recordDamageWriteOff(DamageRecord damageRecord) {
        BigDecimal amount = damageRecord.getItems().stream()
                .filter(item -> item.getDisposition() == DamageDispositionType.WRITE_OFF)
                .map(item -> stockMovementRepository.findByReferenceIdOrderByCreatedAtAsc(damageRecord.getId().toString()).stream()
                        .filter(movement -> movement.getProductVariant().getId().equals(item.getProductVariant().getId()))
                        .filter(movement -> movement.getType() == StockMovement.StockMovementType.ADJUSTMENT)
                        .map(StockMovement::getTotalCost)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        amount = scale(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return upsertEvent(FinancialEventType.DAMAGE_WRITE_OFF, "DAMAGE_RECORD", damageRecord.getId().toString(),
                damageRecord.getRecordNumber(), damageRecord.getReference(),
                "Damage write-off completed for " + damageRecord.getRecordNumber(), amount, defaultCurrency(),
                """
                        {"warehouseId":"%s","reasonCode":"%s"}
                        """.formatted(damageRecord.getWarehouse().getId(), damageRecord.getReasonCode()).trim(),
                List.of(
                        line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_WRITE_OFF, "Inventory Write-Off Expense", amount, defaultCurrency(), "Damage write-off recognized"),
                        line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", amount, defaultCurrency(), "Inventory removed from balance")
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialEventDto> getFinancialEvents(PostingStatus postingStatus, FinancialEventType eventType, String sourceDocumentType) {
        Specification<FinancialEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (postingStatus != null) {
                predicates.add(cb.equal(root.get("postingStatus"), postingStatus));
            }
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (StringUtils.hasText(sourceDocumentType)) {
                predicates.add(cb.equal(root.get("sourceDocumentType"), sourceDocumentType));
            }
            query.orderBy(cb.desc(root.get("occurredAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return financialEventRepository.findAll(spec).stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialEventDto getFinancialEvent(UUID id) {
        return mapToDto(financialEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialEvent", "id", id)));
    }

    @Override
    @Transactional
    public FinancialEventDto retryFinancialEvent(UUID id) {
        FinancialEvent event = financialEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialEvent", "id", id));
        if (event.getPostingStatus() != PostingStatus.FAILED) {
            throw new BadRequestException("Only failed financial events can be retried");
        }
        event.setPostingStatus(PostingStatus.PENDING);
        event.setFailureReason(null);
        event.getSubledgerEntries().forEach(entry -> entry.setPostingStatus(PostingStatus.PENDING));
        return mapToDto(financialEventRepository.save(event));
    }

    private FinancialEventDto upsertEvent(FinancialEventType eventType,
                                          String sourceDocumentType,
                                          String sourceDocumentId,
                                          String sourceDocumentNumber,
                                          String externalReference,
                                          String summary,
                                          BigDecimal totalAmount,
                                          String currency,
                                          String metadataJson,
                                          List<LineDefinition> lines) {
        if (!isPostingEnabled()) {
            return null;
        }

        FinancialEvent event = financialEventRepository.findBySourceDocumentTypeAndSourceDocumentId(sourceDocumentType, sourceDocumentId)
                .orElseGet(FinancialEvent::new);

        event.setEventNumber(event.getId() == null ? generateEventNumber(eventType) : event.getEventNumber());
        event.setEventType(eventType);
        event.setSourceDocumentType(sourceDocumentType);
        event.setSourceDocumentId(sourceDocumentId);
        event.setSourceDocumentNumber(sourceDocumentNumber);
        event.setExternalReference(externalReference);
        event.setSummary(summary);
        event.setTotalAmount(scale(totalAmount));
        event.setCurrency(defaultCurrency(currency));
        event.setPostingStatus(PostingStatus.PENDING);
        event.setFailureReason(null);
        event.setOccurredAt(LocalDateTime.now());
        event.setActorName(resolveActor());
        event.setMetadataJson(metadataJson);
        event.getSubledgerEntries().clear();

        int lineNumber = 1;
        for (LineDefinition definition : lines) {
            SubledgerEntry entry = new SubledgerEntry();
            entry.setFinancialEvent(event);
            entry.setLineNumber(lineNumber++);
            entry.setEntryType(definition.entryType());
            entry.setAccountCode(definition.accountCode());
            entry.setAccountName(definition.accountName());
            entry.setDescription(definition.description());
            entry.setAmount(scale(definition.amount()));
            entry.setCurrency(defaultCurrency(definition.currency()));
            entry.setSourceDocumentType(sourceDocumentType);
            entry.setSourceDocumentId(sourceDocumentId);
            entry.setSourceDocumentNumber(sourceDocumentNumber);
            entry.setPostingStatus(PostingStatus.PENDING);
            event.getSubledgerEntries().add(entry);
        }

        if (!isBalanced(lines)) {
            event.setPostingStatus(PostingStatus.FAILED);
            event.setFailureReason("Subledger entries are not balanced");
            event.getSubledgerEntries().forEach(entry -> entry.setPostingStatus(PostingStatus.FAILED));
        }

        return mapToDto(financialEventRepository.save(event));
    }

    private String generateEventNumber(FinancialEventType eventType) {
        String typeCode = eventType.name().substring(0, Math.min(eventType.name().length(), 4)).toUpperCase(Locale.ROOT);
        return "FE-" + typeCode + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication != null && authentication.isAuthenticated() ? authentication.getName() : "system";
        return TenantContext.getTenantId() + ":" + principal;
    }

    private BigDecimal sumGoodsReceiptAmount(List<GoodsReceiptNoteItem> items) {
        return items.stream()
                .map(item -> BigDecimal.valueOf(item.getAcceptedQuantity()).multiply(item.getPurchaseOrderItem().getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal stockTransactionItemCost(StockTransactionItem item) {
        if (item.getUnitCost() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return scale(item.getUnitCost().multiply(item.getQuantity()));
    }

    private BigDecimal supplierReturnCost(SupplierReturnItem item) {
        if (item.getUnitCost() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return scale(item.getUnitCost().multiply(item.getQuantity()));
    }

    private BigDecimal sumMovementCost(List<StockMovement> movements) {
        return movements.stream()
                .map(StockMovement::getTotalCost)
                .filter(java.util.Objects::nonNull)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumMovementCostByQuantitySign(List<StockMovement> movements, boolean positive) {
        return movements.stream()
                .filter(movement -> positive
                        ? movement.getQuantity().compareTo(BigDecimal.ZERO) > 0
                        : movement.getQuantity().compareTo(BigDecimal.ZERO) < 0)
                .map(StockMovement::getTotalCost)
                .filter(java.util.Objects::nonNull)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildStockTransactionMetadata(StockTransaction stockTransaction, List<StockMovement> movements) {
        return """
                {"type":"%s","sourceWarehouseId":"%s","destinationWarehouseId":"%s","movementCount":%d}
                """.formatted(
                stockTransaction.getType(),
                stockTransaction.getSourceWarehouse() == null ? "" : stockTransaction.getSourceWarehouse().getId(),
                stockTransaction.getDestinationWarehouse() == null ? "" : stockTransaction.getDestinationWarehouse().getId(),
                movements.size()
        ).trim();
    }

    private LineDefinition line(SubledgerEntryType entryType, String accountCode, String accountName, BigDecimal amount, String currency, String description) {
        return new LineDefinition(entryType, accountCode, accountName, scale(amount), defaultCurrency(currency), description);
    }

    private String defaultCurrency() {
        return tenantSettingService.findSetting(VALUATION_CURRENCY_KEY)
                .map(setting -> setting.getValue())
                .filter(StringUtils::hasText)
                .or(() -> tenantSettingService.findSetting(DEFAULT_CURRENCY_KEY)
                        .map(setting -> setting.getValue())
                        .filter(StringUtils::hasText))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse("USD");
    }

    private String defaultCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : defaultCurrency();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : value.setScale(6, RoundingMode.HALF_UP);
    }

    private FinancialEventDto mapToDto(FinancialEvent event) {
        FinancialEventDto dto = new FinancialEventDto();
        dto.setId(event.getId());
        dto.setEventNumber(event.getEventNumber());
        dto.setEventType(event.getEventType());
        dto.setSourceDocumentType(event.getSourceDocumentType());
        dto.setSourceDocumentId(event.getSourceDocumentId());
        dto.setSourceDocumentNumber(event.getSourceDocumentNumber());
        dto.setExternalReference(event.getExternalReference());
        dto.setSummary(event.getSummary());
        dto.setTotalAmount(event.getTotalAmount());
        dto.setCurrency(event.getCurrency());
        dto.setPostingStatus(event.getPostingStatus());
        dto.setFailureReason(event.getFailureReason());
        dto.setOccurredAt(event.getOccurredAt());
        dto.setActorName(event.getActorName());
        dto.setMetadataJson(event.getMetadataJson());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setSubledgerEntries(event.getSubledgerEntries().stream().map(this::mapLineToDto).toList());
        return dto;
    }

    private boolean isPostingEnabled() {
        return tenantSettingService.findSetting(ACCOUNTING_POSTING_ENABLED_KEY)
                .map(setting -> Boolean.parseBoolean(setting.getValue()))
                .orElse(true);
    }

    private boolean isBalanced(List<LineDefinition> lines) {
        BigDecimal debits = lines.stream()
                .filter(line -> line.entryType() == SubledgerEntryType.DEBIT)
                .map(LineDefinition::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.stream()
                .filter(line -> line.entryType() == SubledgerEntryType.CREDIT)
                .map(LineDefinition::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return debits.compareTo(credits) == 0;
    }

    private SubledgerEntryDto mapLineToDto(SubledgerEntry entry) {
        SubledgerEntryDto dto = new SubledgerEntryDto();
        dto.setId(entry.getId());
        dto.setLineNumber(entry.getLineNumber());
        dto.setEntryType(entry.getEntryType());
        dto.setAccountCode(entry.getAccountCode());
        dto.setAccountName(entry.getAccountName());
        dto.setDescription(entry.getDescription());
        dto.setAmount(entry.getAmount());
        dto.setCurrency(entry.getCurrency());
        dto.setSourceDocumentType(entry.getSourceDocumentType());
        dto.setSourceDocumentId(entry.getSourceDocumentId());
        dto.setSourceDocumentNumber(entry.getSourceDocumentNumber());
        dto.setPostingStatus(entry.getPostingStatus());
        return dto;
    }

    private record LineDefinition(SubledgerEntryType entryType,
                                  String accountCode,
                                  String accountName,
                                  BigDecimal amount,
                                  String currency,
                                  String description) {
    }
}
