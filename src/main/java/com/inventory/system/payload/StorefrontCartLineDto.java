package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontCartLineDto {
    private UUID productVariantId;
    private String productSlug;
    private String sku;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal baseUnitPrice;
    private BigDecimal finalUnitPrice;
    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;
    private BigDecimal availableToPromise;
    private String imageUrl;
}
