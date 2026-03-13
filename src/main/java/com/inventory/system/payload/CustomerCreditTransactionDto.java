package com.inventory.system.payload;

import com.inventory.system.common.entity.CreditTransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CustomerCreditTransactionDto {
    private UUID id;
    private UUID customerId;
    private CreditTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceNumber;
    private String notes;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
}