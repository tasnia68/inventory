package com.inventory.system.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SuspendedPosSaleItemRequest {

    @NotNull
    private UUID productVariantId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    private BigDecimal lineDiscount = BigDecimal.ZERO;
}