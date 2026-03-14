package com.inventory.system.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PricingPreviewItemRequest {
    @NotNull
    private UUID productVariantId;

    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal manualLineDiscount = BigDecimal.ZERO;
}