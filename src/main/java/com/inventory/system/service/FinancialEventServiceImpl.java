package com.inventory.system.service;

import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageRecord;
import com.inventory.system.common.entity.FinancialEvent;
import com.inventory.system.common.entity.FinancialEventType;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.ChartOfAccount;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosSalePayment;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesRefund;
import com.inventory.system.common.entity.ShippingRateCard;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private static final String ACCOUNTING_AUTO_POST_EVENTS_KEY = "accounting.auto_post_events";
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
    private static final String ACCOUNT_COURIER_FEE_EXPENSE = "COURIER_FEE_EXPENSE";
    private static final String ACCOUNT_COD_FEE_EXPENSE = "COD_FEE_EXPENSE";
    private static final String ACCOUNT_ACCRUED_COURIER_PAYABLE = "ACCRUED_COURIER_PAYABLE";
    private static final String ACCOUNT_BANK_DEPOSIT = "BANK_DEPOSIT";
    private static final String ACCOUNT_TAX_PAYABLE = "TAX_PAYABLE";

    private final FinancialEventRepository financialEventRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TenantSettingService tenantSettingService;
    private final com.inventory.system.repository.ShippingRateCardRepository shippingRateCardRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public FinancialEventDto recordFinancialEvent(com.inventory.system.accounting.api.event.FinancialEventDto event) {
        if (event == null) {
            throw new BadRequestException("Financial event is required");
        }
        if (!StringUtils.hasText(event.getEventType())
                || !StringUtils.hasText(event.getSourceDocumentType())
                || !StringUtils.hasText(event.getSourceDocumentId())
                || !StringUtils.hasText(event.getSummary())) {
            throw new BadRequestException("Event type, source document type, source document id, and summary are required");
        }
        if (event.getLines() == null || event.getLines().isEmpty()) {
            throw new BadRequestException("Financial event must include at least one accounting line");
        }

        FinancialEventType eventType = parseEventType(event.getEventType());
        List<LineDefinition> lines = event.getLines().stream()
                .map(line -> line(
                        parseEntryType(line.getEntryType()),
                        requiredText(line.getAccountCode(), "Line account code is required"),
                        requiredText(line.getAccountName(), "Line account name is required"),
                        line.getAmount(),
                        StringUtils.hasText(line.getCurrency()) ? line.getCurrency() : event.getCurrency(),
                        line.getDescription()
                ))
                .toList();

        return upsertEvent(
                eventType,
                event.getSourceDocumentType(),
                event.getSourceDocumentId(),
                event.getSourceDocumentNumber(),
                event.getExternalReference(),
                event.getSummary(),
                event.getTotalAmount(),
                event.getCurrency(),
                event.getMetadataJson(),
                lines
        );
    }

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
        BigDecimal taxAmount = scale(posSale.getTaxAmount());
        BigDecimal netRevenue = revenueAmount.subtract(taxAmount).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
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
        lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_SALES_REVENUE, "Sales Revenue", netRevenue, posSale.getCurrency(), "POS sale revenue net of tax"));
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount outputTaxAccount = posSale.getTaxRate() == null ? null : posSale.getTaxRate().getOutputAccount();
            lines.add(line(
                    SubledgerEntryType.CREDIT,
                    outputTaxAccount == null ? ACCOUNT_TAX_PAYABLE : outputTaxAccount.getAccountCode(),
                    outputTaxAccount == null ? "Tax Payable" : outputTaxAccount.getAccountName(),
                    taxAmount,
                    posSale.getCurrency(),
                    posSale.getTaxRate() == null ? "POS output tax" : "POS output tax " + posSale.getTaxRate().getCode()
            ));
        }
        if (costAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_COGS, "Cost of Goods Sold", costAmount, posSale.getCurrency(), "Cost of goods sold for POS sale"));
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset", costAmount, posSale.getCurrency(), "Inventory relieved for POS sale"));
        }

        return upsertEvent(FinancialEventType.POS_SALE, "POS_SALE", posSale.getId().toString(), posSale.getReceiptNumber(),
                posSale.getSalesOrder() == null ? posSale.getReceiptNumber() : posSale.getSalesOrder().getSoNumber(),
                "POS sale completed for receipt " + posSale.getReceiptNumber(), revenueAmount, posSale.getCurrency(),
                """
                        {"warehouseId":"%s","terminalId":"%s","paymentMethod":"%s","taxRateId":"%s"}
                        """.formatted(posSale.getWarehouse().getId(), posSale.getTerminal().getId(), posSale.getPaymentMethod(),
                        posSale.getTaxRate() == null ? "" : posSale.getTaxRate().getId()).trim(),
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
    public FinancialEventDto recordSalesOrderDelivery(SalesOrder salesOrder, boolean partialDelivery) {
        BigDecimal revenueAmount = partialDelivery
                ? sumPartialLineRevenue(salesOrder)
                : scale(salesOrder.getTotalAmount());
        if (revenueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal cogsAmount = computeCogsForDelivery(salesOrder, partialDelivery);
        String currency = salesOrder.getCurrency() != null ? salesOrder.getCurrency() : defaultCurrency();

        // Split tax out as a separate liability so revenue is reported net of tax.
        // For partial delivery, prorate the order-level tax against the delivered fraction
        // of the subtotal; for full delivery, use the captured taxAmount as-is.
        BigDecimal taxAmount = computeRecognisedTax(salesOrder, revenueAmount, partialDelivery);
        BigDecimal netRevenue = revenueAmount.subtract(taxAmount).max(BigDecimal.ZERO);

        List<LineDefinition> lines = new ArrayList<>();
        lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing",
                revenueAmount, currency,
                partialDelivery ? "COD collected for partial delivery" : "COD collected on delivery"));
        lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_SALES_REVENUE, "Sales Revenue",
                netRevenue, currency,
                partialDelivery ? "Sales revenue (net of tax) for delivered portion" : "Sales revenue (net of tax) on delivery"));
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_TAX_PAYABLE, "Tax Payable",
                    taxAmount, currency,
                    partialDelivery ? "Output tax accrued for delivered portion" : "Output tax accrued on delivery"));
        }
        if (cogsAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_COGS, "Cost of Goods Sold",
                    cogsAmount, currency,
                    partialDelivery ? "COGS for delivered portion" : "COGS for delivered order"));
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset",
                    cogsAmount, currency,
                    "Inventory relieved for delivered SO " + salesOrder.getSoNumber()));
        }

        FinancialEventType type = partialDelivery
                ? FinancialEventType.SALES_ORDER_PARTIAL_DELIVERY
                : FinancialEventType.SALES_ORDER_DELIVERY;
        return upsertEvent(type, "SALES_ORDER", salesOrder.getId().toString(), salesOrder.getSoNumber(),
                salesOrder.getSoNumber(),
                (partialDelivery ? "Partial delivery recognised for " : "Delivery recognised for ") + salesOrder.getSoNumber(),
                revenueAmount, currency,
                """
                        {"salesOrderId":"%s","courierProfileId":"%s","deliveryZone":"%s","partial":%s,"cogs":"%s"}
                        """.formatted(
                                salesOrder.getId(),
                                salesOrder.getCourierProfileId(),
                                salesOrder.getDeliveryZone(),
                                partialDelivery,
                                cogsAmount).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordCourierFee(SalesOrder salesOrder, boolean partialDelivery) {
        ShippingRateCard card = lookupRateCard(salesOrder);
        if (card == null) return null;
        BigDecimal courierCost = card.getCourierCost() != null ? scale(card.getCourierCost()) : BigDecimal.ZERO;
        if (courierCost.compareTo(BigDecimal.ZERO) <= 0) return null;
        String currency = salesOrder.getCurrency() != null ? salesOrder.getCurrency() : defaultCurrency();
        List<LineDefinition> lines = List.of(
                line(SubledgerEntryType.DEBIT, ACCOUNT_COURIER_FEE_EXPENSE, "Courier Fee Expense",
                        courierCost, currency, "Courier fee for SO " + salesOrder.getSoNumber()),
                line(SubledgerEntryType.CREDIT, ACCOUNT_ACCRUED_COURIER_PAYABLE, "Accrued Courier Payable",
                        courierCost, currency, "Amount owed to courier for SO " + salesOrder.getSoNumber())
        );
        return upsertEvent(FinancialEventType.COURIER_FEE, "SALES_ORDER",
                salesOrder.getId().toString() + ":courier-fee", salesOrder.getSoNumber(),
                salesOrder.getSoNumber(), "Courier fee accrual for " + salesOrder.getSoNumber(),
                courierCost, currency,
                """
                        {"salesOrderId":"%s","courierProfileId":"%s","deliveryZone":"%s","partial":%s}
                        """.formatted(salesOrder.getId(), salesOrder.getCourierProfileId(),
                                salesOrder.getDeliveryZone(), partialDelivery).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordCodCollectionFee(SalesOrder salesOrder, boolean partialDelivery) {
        ShippingRateCard card = lookupRateCard(salesOrder);
        if (card == null) return null;
        BigDecimal feePercent = card.getCodFeePercent() != null ? card.getCodFeePercent() : BigDecimal.ZERO;
        if (feePercent.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal codBase;
        if (partialDelivery) {
            codBase = sumPartialLineRevenue(salesOrder);
        } else {
            codBase = salesOrder.getCodAmount() != null ? salesOrder.getCodAmount()
                    : (salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount() : BigDecimal.ZERO);
        }
        if (codBase.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal codFee = scale(codBase.multiply(feePercent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if (codFee.compareTo(BigDecimal.ZERO) <= 0) return null;

        String currency = salesOrder.getCurrency() != null ? salesOrder.getCurrency() : defaultCurrency();
        List<LineDefinition> lines = List.of(
                line(SubledgerEntryType.DEBIT, ACCOUNT_COD_FEE_EXPENSE, "COD Collection Fee Expense",
                        codFee, currency, "COD collection fee for SO " + salesOrder.getSoNumber()),
                line(SubledgerEntryType.CREDIT, ACCOUNT_ACCRUED_COURIER_PAYABLE, "Accrued Courier Payable",
                        codFee, currency, "COD fee owed to courier for SO " + salesOrder.getSoNumber())
        );
        return upsertEvent(FinancialEventType.COD_COLLECTION_FEE, "SALES_ORDER",
                salesOrder.getId().toString() + ":cod-fee", salesOrder.getSoNumber(),
                salesOrder.getSoNumber(), "COD collection fee for " + salesOrder.getSoNumber(),
                codFee, currency,
                """
                        {"salesOrderId":"%s","feePercent":"%s","codBase":"%s"}
                        """.formatted(salesOrder.getId(), feePercent, codBase).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordSalesOrderReturn(SalesOrder salesOrder) {
        BigDecimal recognizedRevenue = sumPriorRecognizedRevenue(salesOrder);
        BigDecimal recognizedCogs = sumPriorRecognizedCogs(salesOrder);
        if (recognizedRevenue.compareTo(BigDecimal.ZERO) <= 0 && recognizedCogs.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String currency = salesOrder.getCurrency() != null ? salesOrder.getCurrency() : defaultCurrency();
        List<LineDefinition> lines = new ArrayList<>();
        if (recognizedRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recognizedTax = computeRecognisedTax(salesOrder, recognizedRevenue, true);
            BigDecimal recognizedNet = recognizedRevenue.subtract(recognizedTax).max(BigDecimal.ZERO);
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_SALES_RETURNS, "Sales Returns and Allowances",
                    recognizedNet, currency, "Reversal of revenue (net of tax) on return for " + salesOrder.getSoNumber()));
            if (recognizedTax.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_TAX_PAYABLE, "Tax Payable",
                        recognizedTax, currency, "Reversal of output tax on return for " + salesOrder.getSoNumber()));
            }
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing",
                    recognizedRevenue, currency, "Cash refund obligation for returned SO " + salesOrder.getSoNumber()));
        }
        if (recognizedCogs.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_INVENTORY_ASSET, "Inventory Asset",
                    recognizedCogs, currency, "Restocked inventory on return"));
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_COGS, "Cost of Goods Sold",
                    recognizedCogs, currency, "COGS reversal on return"));
        }
        BigDecimal totalAmount = recognizedRevenue.add(recognizedCogs);
        return upsertEvent(FinancialEventType.SALES_ORDER_RETURN, "SALES_ORDER",
                salesOrder.getId().toString() + ":return", salesOrder.getSoNumber(),
                salesOrder.getSoNumber(), "Return reversal for " + salesOrder.getSoNumber(),
                totalAmount, currency,
                """
                        {"salesOrderId":"%s","reversedRevenue":"%s","reversedCogs":"%s"}
                        """.formatted(salesOrder.getId(), recognizedRevenue, recognizedCogs).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordSalesOrderPartialCancel(SalesOrder salesOrder) {
        BigDecimal shippingAmount = salesOrder.getShippingAmount() != null
                ? scale(salesOrder.getShippingAmount()) : BigDecimal.ZERO;
        if (shippingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String currency = salesOrder.getCurrency() != null ? salesOrder.getCurrency() : defaultCurrency();
        List<LineDefinition> lines = List.of(
                line(SubledgerEntryType.DEBIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing",
                        shippingAmount, currency, "Delivery fee collected (goods refused)"),
                line(SubledgerEntryType.CREDIT, ACCOUNT_SALES_REVENUE, "Sales Revenue",
                        shippingAmount, currency, "Shipping revenue recognised on partial cancel")
        );
        return upsertEvent(FinancialEventType.SALES_ORDER_DELIVERY, "SALES_ORDER",
                salesOrder.getId().toString() + ":partial-cancel", salesOrder.getSoNumber(),
                salesOrder.getSoNumber(),
                "Partial cancel — delivery fee retained for " + salesOrder.getSoNumber(),
                shippingAmount, currency,
                """
                        {"salesOrderId":"%s","scenario":"partial_cancel","shippingAmount":"%s"}
                        """.formatted(salesOrder.getId(), shippingAmount).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordTaxRemittance(String referenceNumber, BigDecimal amount, String currency, String notes) {
        BigDecimal scaledAmount = scale(amount != null ? amount : BigDecimal.ZERO);
        if (scaledAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.inventory.system.common.exception.BadRequestException("Tax remittance amount must be greater than zero");
        }
        String resolvedCurrency = currency != null && !currency.isBlank() ? currency : defaultCurrency();
        String reference = referenceNumber != null && !referenceNumber.isBlank()
                ? referenceNumber.trim()
                : "TAX-" + java.time.LocalDate.now();
        List<LineDefinition> lines = List.of(
                line(SubledgerEntryType.DEBIT, ACCOUNT_TAX_PAYABLE, "Tax Payable",
                        scaledAmount, resolvedCurrency, "Tax filing remittance " + reference),
                line(SubledgerEntryType.CREDIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing",
                        scaledAmount, resolvedCurrency, "Cash paid for tax filing " + reference)
        );
        return upsertEvent(FinancialEventType.TAX_REMITTANCE, "TAX_REMITTANCE",
                reference, reference, reference,
                notes != null && !notes.isBlank() ? notes : "Tax remittance " + reference,
                scaledAmount, resolvedCurrency,
                """
                        {"reference":"%s","amount":"%s"}
                        """.formatted(reference, scaledAmount).trim(),
                lines);
    }

    @Override
    @Transactional
    public FinancialEventDto recordCourierSettlement(String settlementReference, java.util.UUID courierProfileId,
                                                     BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount,
                                                     String currency, String notes) {
        BigDecimal gross = scale(grossAmount != null ? grossAmount : BigDecimal.ZERO);
        BigDecimal fee = scale(feeAmount != null ? feeAmount : BigDecimal.ZERO);
        BigDecimal net = scale(netAmount != null ? netAmount : gross.subtract(fee));
        if (gross.compareTo(BigDecimal.ZERO) <= 0 && net.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String resolvedCurrency = defaultCurrency(currency);
        List<LineDefinition> lines = new ArrayList<>();
        if (net.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_BANK_DEPOSIT, "Bank Deposit",
                    net, resolvedCurrency, "Cash received from courier remittance"));
        }
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.DEBIT, ACCOUNT_ACCRUED_COURIER_PAYABLE, "Accrued Courier Payable",
                    fee, resolvedCurrency, "Settling accrued courier fees"));
        }
        BigDecimal credit = net.add(fee);
        if (credit.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(SubledgerEntryType.CREDIT, ACCOUNT_CASH_CLEARING, "Cash and Payment Clearing",
                    credit, resolvedCurrency, "Clearing courier-held COD on settlement"));
        }
        return upsertEvent(FinancialEventType.COURIER_FEE,
                "COURIER_SETTLEMENT", settlementReference, settlementReference, settlementReference,
                notes != null ? notes : "Courier settlement " + settlementReference,
                net, resolvedCurrency,
                """
                        {"courierProfileId":"%s","gross":"%s","fee":"%s","net":"%s"}
                        """.formatted(courierProfileId, gross, fee, net).trim(),
                lines);
    }

    private ShippingRateCard lookupRateCard(SalesOrder salesOrder) {
        if (salesOrder.getCourierProfileId() == null || salesOrder.getDeliveryZone() == null) return null;
        return shippingRateCardRepository
                .findFirstByCourierProfileIdAndZone(salesOrder.getCourierProfileId(), salesOrder.getDeliveryZone())
                .orElse(null);
    }

    private BigDecimal computeCogsForDelivery(SalesOrder salesOrder, boolean partialDelivery) {
        BigDecimal totalCogs = BigDecimal.ZERO;
        for (SalesOrderItem item : salesOrder.getItems()) {
            String ref = salesOrder.getId().toString() + ":ship:" + item.getId();
            List<StockMovement> movements = stockMovementRepository.findByReferenceIdOrderByCreatedAtAsc(ref);
            BigDecimal itemCogs = sumMovementCost(movements);
            if (partialDelivery) {
                BigDecimal shipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
                BigDecimal fulfilled = item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : BigDecimal.ZERO;
                if (shipped.compareTo(BigDecimal.ZERO) > 0 && itemCogs.compareTo(BigDecimal.ZERO) > 0) {
                    itemCogs = itemCogs.multiply(fulfilled).divide(shipped, 6, RoundingMode.HALF_UP);
                } else {
                    itemCogs = BigDecimal.ZERO;
                }
            }
            totalCogs = totalCogs.add(itemCogs);
        }
        return scale(totalCogs);
    }

    private BigDecimal sumPriorRecognizedRevenue(SalesOrder salesOrder) {
        java.util.Set<FinancialEventType> revenueTypes = java.util.EnumSet.of(
                FinancialEventType.SALES_ORDER_DELIVERY, FinancialEventType.SALES_ORDER_PARTIAL_DELIVERY);
        return financialEventRepository.findAll().stream()
                .filter(e -> "SALES_ORDER".equals(e.getSourceDocumentType()))
                .filter(e -> salesOrder.getId().toString().equals(e.getSourceDocumentId()))
                .filter(e -> revenueTypes.contains(e.getEventType()))
                .map(FinancialEvent::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPriorRecognizedCogs(SalesOrder salesOrder) {
        BigDecimal totalCogs = BigDecimal.ZERO;
        for (SalesOrderItem item : salesOrder.getItems()) {
            String ref = salesOrder.getId().toString() + ":ship:" + item.getId();
            List<StockMovement> movements = stockMovementRepository.findByReferenceIdOrderByCreatedAtAsc(ref);
            totalCogs = totalCogs.add(sumMovementCost(movements));
        }
        return scale(totalCogs);
    }

    private BigDecimal sumPartialLineRevenue(SalesOrder salesOrder) {
        BigDecimal linesTotal = salesOrder.getItems().stream()
                .map(this::fulfilledLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = salesOrder.getShippingAmount() != null ? salesOrder.getShippingAmount() : BigDecimal.ZERO;
        return scale(linesTotal.add(shipping));
    }

    private BigDecimal computeRecognisedTax(SalesOrder salesOrder, BigDecimal recognisedRevenue, boolean partialDelivery) {
        BigDecimal orderTax = salesOrder.getTaxAmount() != null ? salesOrder.getTaxAmount() : BigDecimal.ZERO;
        if (orderTax.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (!partialDelivery) {
            return scale(orderTax);
        }
        BigDecimal orderTotal = salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount() : BigDecimal.ZERO;
        if (orderTotal.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return scale(orderTax.multiply(recognisedRevenue).divide(orderTotal, 6, java.math.RoundingMode.HALF_UP));
    }

    private BigDecimal fulfilledLineTotal(SalesOrderItem item) {
        BigDecimal fulfilled = item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : BigDecimal.ZERO;
        BigDecimal unit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        return fulfilled.multiply(unit);
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
        FinancialEvent saved = financialEventRepository.save(event);
        if (saved.getPostingStatus() == PostingStatus.PENDING && isAutoPostEnabled()) {
            applicationEventPublisher.publishEvent(new FinancialEventRecordedEvent(saved.getId()));
        }
        return mapToDto(saved);
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

        FinancialEvent saved = financialEventRepository.save(event);
        if (saved.getPostingStatus() == PostingStatus.PENDING && isAutoPostEnabled()) {
            applicationEventPublisher.publishEvent(new FinancialEventRecordedEvent(saved.getId()));
        }
        return mapToDto(saved);
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

    private FinancialEventType parseEventType(String value) {
        try {
            return FinancialEventType.valueOf(requiredText(value, "Event type is required").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported financial event type: " + value);
        }
    }

    private SubledgerEntryType parseEntryType(String value) {
        try {
            return SubledgerEntryType.valueOf(requiredText(value, "Line entry type is required").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported financial event line entry type: " + value);
        }
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
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

    private boolean isAutoPostEnabled() {
        return tenantSettingService.findSetting(ACCOUNTING_AUTO_POST_EVENTS_KEY)
                .map(setting -> Boolean.parseBoolean(setting.getValue()))
                .orElse(false);
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
