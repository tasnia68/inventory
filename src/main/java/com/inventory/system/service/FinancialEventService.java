package com.inventory.system.service;

import com.inventory.system.accounting.api.event.FinancialEventSource;
import com.inventory.system.common.entity.DamageRecord;
import com.inventory.system.common.entity.FinancialEventType;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesRefund;
import com.inventory.system.common.entity.StockTransaction;
import com.inventory.system.common.entity.SupplierReturn;
import com.inventory.system.payload.FinancialEventDto;

import java.util.List;
import java.util.UUID;

public interface FinancialEventService extends FinancialEventSource {
    FinancialEventDto recordGoodsReceipt(GoodsReceiptNote goodsReceiptNote);
    FinancialEventDto recordStockTransaction(StockTransaction stockTransaction);
    FinancialEventDto recordPosSale(PosSale posSale);
    FinancialEventDto recordSalesRefund(SalesRefund salesRefund);
    FinancialEventDto recordSalesOrderDelivery(SalesOrder salesOrder, boolean partialDelivery);
    FinancialEventDto recordCourierFee(SalesOrder salesOrder, boolean partialDelivery);
    FinancialEventDto recordCodCollectionFee(SalesOrder salesOrder, boolean partialDelivery);
    FinancialEventDto recordSalesOrderReturn(SalesOrder salesOrder);
    FinancialEventDto recordSalesOrderPartialCancel(SalesOrder salesOrder);
    FinancialEventDto recordCourierSettlement(String settlementReference, java.util.UUID courierProfileId, java.math.BigDecimal grossAmount, java.math.BigDecimal feeAmount, java.math.BigDecimal netAmount, String currency, String notes);
    FinancialEventDto recordTaxRemittance(String referenceNumber, java.math.BigDecimal amount, String currency, String notes);
    FinancialEventDto recordSupplierReturn(SupplierReturn supplierReturn);
    FinancialEventDto recordDamageWriteOff(DamageRecord damageRecord);
    List<FinancialEventDto> getFinancialEvents(PostingStatus postingStatus, FinancialEventType eventType, String sourceDocumentType);
    FinancialEventDto getFinancialEvent(UUID id);
    FinancialEventDto retryFinancialEvent(UUID id);
}
