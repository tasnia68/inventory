package com.inventory.system.payload;

import com.inventory.system.common.entity.PosCashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosCashMovementRequest {

    @NotNull
    private PosCashMovementType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    private String reason;

    private String referenceNumber;
    private String notes;
}