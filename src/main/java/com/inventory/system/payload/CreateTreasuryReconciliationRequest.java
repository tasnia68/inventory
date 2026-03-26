package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateTreasuryReconciliationRequest {
    private UUID treasuryAccountId;
    private LocalDate businessDate;
    private BigDecimal statementBalance;
    private String notes;
}
