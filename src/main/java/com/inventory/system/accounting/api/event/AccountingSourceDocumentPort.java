package com.inventory.system.accounting.api.event;

import com.inventory.system.common.entity.ReconciliationSourceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountingSourceDocumentPort {
    PayableSource resolvePayableSource(UUID supplierId, UUID purchaseOrderId);

    ReceivableSource resolveReceivableSource(UUID customerId, UUID salesOrderId);

    String assertThreeWayMatch(UUID purchaseOrderId, BigDecimal invoiceTotal, boolean force);

    List<CashReconciliationSeed> findCashReconciliationSeeds(LocalDate businessDate);

    record PayableSource(
            UUID supplierId,
            String supplierName,
            UUID purchaseOrderId,
            String purchaseOrderNumber,
            BigDecimal defaultTotalAmount,
            String defaultCurrency) {
    }

    record ReceivableSource(
            UUID customerId,
            String customerName,
            UUID salesOrderId,
            String salesOrderNumber,
            BigDecimal defaultTotalAmount,
            String defaultCurrency) {
    }

    record CashReconciliationSeed(
            ReconciliationSourceType sourceType,
            String sourceId,
            String sourceReference,
            LocalDate transactionDate,
            String description,
            BigDecimal amount) {
    }
}