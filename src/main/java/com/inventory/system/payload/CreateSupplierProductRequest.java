package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateSupplierProductRequest {
    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @NotNull(message = "Product Variant ID is required")
    private UUID productVariantId;

    private String supplierSku;

    private BigDecimal price;

    private String currency;

    private Integer leadTimeDays;
}
