package com.inventory.system.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PosSaleItemRequest {
    @NotNull
    private UUID productVariantId;

    @NotNull
    @DecimalMin(value = "0.000001")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal unitPrice;

    private BigDecimal lineDiscount = BigDecimal.ZERO;
}