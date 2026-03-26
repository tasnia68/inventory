package com.inventory.system.payload;

import com.inventory.system.common.entity.ReconciliationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TreasuryReconciliationDto {
    private UUID id;
    private UUID treasuryAccountId;
    private String treasuryAccountCode;
    private String treasuryAccountName;
    private LocalDate businessDate;
    private ReconciliationStatus status;
    private BigDecimal statementBalance;
    private BigDecimal systemBalance;
    private BigDecimal differenceAmount;
    private String notes;
    private LocalDateTime completedAt;
    private List<TreasuryReconciliationLineDto> lines = new ArrayList<>();
}
