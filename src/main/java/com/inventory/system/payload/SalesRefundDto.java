package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import com.inventory.system.common.entity.RefundMethod;
import com.inventory.system.common.entity.SalesRefundStatus;
import com.inventory.system.common.entity.SalesRefundType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SalesRefundDto {
    private UUID id;
    private String refundNumber;
    private String creditNoteNumber;
    private UUID salesOrderId;
    private String soNumber;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private UUID rmaId;
    private String rmaNumber;
    private UUID posSaleId;
    private String receiptNumber;
    private UUID replacementSalesOrderId;
    private String replacementSalesOrderNumber;
    private SalesRefundStatus status;
    private SalesRefundType refundType;
    private RefundMethod refundMethod;
    private PosPaymentMethod originalPaymentMethod;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime cancelledAt;
    private String reason;
    private String notes;
    private BigDecimal subtotalAmount;
    private BigDecimal replacementAmount;
    private BigDecimal netRefundAmount;
    private BigDecimal amountDueFromCustomer;
    private BigDecimal storeCreditIssued;
    private BigDecimal exchangePriceDifference;
    private LocalDateTime documentGeneratedAt;
    private List<SalesRefundItemDto> items;
    private List<SalesRefundAuditEntryDto> auditEntries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}