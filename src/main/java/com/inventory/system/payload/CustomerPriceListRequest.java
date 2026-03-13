package com.inventory.system.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CustomerPriceListRequest {
    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.000001", message = "Price must be greater than 0")
    private BigDecimal price;

    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
}