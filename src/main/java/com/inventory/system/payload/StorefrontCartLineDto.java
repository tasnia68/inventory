package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontCartLineDto {
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
