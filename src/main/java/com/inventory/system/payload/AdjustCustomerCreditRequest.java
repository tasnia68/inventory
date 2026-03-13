package com.inventory.system.payload;

import com.inventory.system.common.entity.CreditTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustCustomerCreditRequest {
    @NotNull(message = "Transaction type is required")
    private CreditTransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.000001", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String referenceNumber;
    private String notes;
}