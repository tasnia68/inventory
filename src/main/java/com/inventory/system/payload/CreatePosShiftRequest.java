package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePosShiftRequest {
    @NotNull
    private UUID terminalId;

    private BigDecimal openingFloat = BigDecimal.ZERO;
}