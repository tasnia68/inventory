package com.inventory.system.payload;

import com.inventory.system.common.entity.PosCashMovementType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PosCashMovementDto {
    private UUID id;
    private UUID shiftId;
    private UUID terminalId;
    private String terminalName;
    private UUID cashierId;
    private String cashierName;
    private PosCashMovementType type;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
    private String reason;
    private String referenceNumber;
    private String notes;
}