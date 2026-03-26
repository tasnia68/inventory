package com.inventory.system.payload;

import com.inventory.system.common.entity.ReconciliationSourceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TreasuryReconciliationLineDto {
    private UUID id;
    private ReconciliationSourceType sourceType;
    private String sourceId;
    private String sourceReference;
    private LocalDate transactionDate;
    private String description;
    private BigDecimal amount;
    private boolean matched;
}
