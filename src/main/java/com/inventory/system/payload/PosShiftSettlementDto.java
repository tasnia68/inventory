package com.inventory.system.payload;

import com.inventory.system.common.entity.PosSettlementApprovalStatus;
import com.inventory.system.common.entity.PosShiftStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PosShiftSettlementDto {
    private UUID shiftId;
    private UUID terminalId;
    private String terminalName;
    private UUID cashierId;
    private String cashierName;
    private PosShiftStatus shiftStatus;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingFloat;
    private BigDecimal totalSales;
    private BigDecimal totalRefunds;
    private BigDecimal totalCashInflows;
    private BigDecimal totalCashOutflows;
    private BigDecimal expectedCashAmount;
    private BigDecimal declaredCashAmount;
    private BigDecimal overShortAmount;
    private PosSettlementApprovalStatus settlementApprovalStatus;
    private LocalDateTime settlementApprovedAt;
    private UUID settlementApprovedBy;
    private String settlementApprovedByName;
    private String settlementApprovalNotes;
    private String closingNotes;
    private List<PosShiftTenderCountDto> tenderCounts;
    private List<PosCashMovementDto> cashMovements;
    private List<PosRefundSettlementImpactDto> refundImpacts;
}