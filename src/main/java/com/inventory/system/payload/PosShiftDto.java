package com.inventory.system.payload;

import com.inventory.system.common.entity.PosShiftStatus;
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
    private String closingNotes;
}