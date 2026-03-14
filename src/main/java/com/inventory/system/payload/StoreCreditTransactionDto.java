package com.inventory.system.payload;

import com.inventory.system.common.entity.StoreCreditTransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StoreCreditTransactionDto {
    private UUID id;
    private UUID customerId;
    private UUID salesRefundId;
    private String refundNumber;
    private StoreCreditTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceNumber;
    private String notes;
    private LocalDateTime transactionDate;
}