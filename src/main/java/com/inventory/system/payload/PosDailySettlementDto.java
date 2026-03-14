package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PosDailySettlementDto {
    private LocalDate businessDate;
    private UUID terminalId;
    private String terminalName;
    private long shiftCount;
    private long openShiftCount;
    private BigDecimal totalSales;
    private BigDecimal totalRefunds;
    private BigDecimal totalCashInflows;
    private BigDecimal totalCashOutflows;
    private BigDecimal expectedCash;
    private BigDecimal declaredCash;
    private BigDecimal overShortAmount;
}