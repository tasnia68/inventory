package com.inventory.system.payload;

import com.inventory.system.common.entity.PosShiftStatus;
import com.inventory.system.common.entity.PosSettlementApprovalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PosShiftDto {
    private UUID id;
    private UUID terminalId;
    private String terminalName;
    private UUID cashierId;
    private String cashierName;
    private PosShiftStatus status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingFloat;
    private BigDecimal expectedCashAmount;
    private BigDecimal declaredCashAmount;
    private BigDecimal overShortAmount;
    private PosSettlementApprovalStatus settlementApprovalStatus;
    private LocalDateTime settlementApprovedAt;
    private UUID settlementApprovedBy;
    private String settlementApprovedByName;
    private String closingNotes;
}