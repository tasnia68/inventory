package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a simple product without variants.
 * This is a convenience API that creates both a template and a single variant in one request.
 */
@Data
public class SimpleProductDto {
    
    // Template fields
    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    
    @NotNull(message = "UOM ID is required")
    private UUID uomId;
    
    private Boolean isBatchTracked = false;
    
    private Boolean isSerialTracked = false;
    
    private Boolean isActive = true;
    
    // Variant fields
    @NotBlank(message = "SKU is required")
    private String sku;
    
    private String barcode;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;
}
