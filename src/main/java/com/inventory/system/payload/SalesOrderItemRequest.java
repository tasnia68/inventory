package com.inventory.system.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SalesOrderItemRequest {

    @NotNull(message = "Product Variant ID is required")
    private UUID productVariantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit Price is required")
    @Min(value = 0, message = "Unit Price cannot be negative")
    private BigDecimal unitPrice;
}
