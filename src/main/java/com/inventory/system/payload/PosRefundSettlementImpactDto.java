package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PosRefundSettlementImpactDto {
    private UUID id;
    private UUID salesRefundId;
    private UUID shiftId;
    private UUID terminalId;
    private PosPaymentMethod paymentMethod;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
    private String referenceNumber;
    private String notes;
}