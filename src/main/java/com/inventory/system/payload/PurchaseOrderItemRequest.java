package com.inventory.system.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseOrderItemRequest {

    @NotNull(message = "Product Variant ID is required")
    private UUID productVariantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit Price is required")
    @Min(value = 0, message = "Unit Price cannot be negative")
    private BigDecimal unitPrice;
}
