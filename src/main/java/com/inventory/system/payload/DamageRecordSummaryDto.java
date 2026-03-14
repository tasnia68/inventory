package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DamageRecordSummaryDto {
    private long totalRecords;
    private long draftRecords;
    private long pendingApprovalRecords;
    private long approvedRecords;
    private long completedRecords;
    private long cancelledRecords;
    private BigDecimal totalDamagedQuantity;
    private BigDecimal quarantineQuantity;
    private BigDecimal writeOffQuantity;
}