package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontVariantOptionDto {
    private String productVariantId;
    private String slug;
    private String sku;
    private String label;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal availableToPromise;
    private String availabilityLabel;
    private Boolean featured;
    private String badge;
}
