package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class PricingPreviewLineDto {
    private UUID productVariantId;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal baseUnitPrice;
    private BigDecimal finalUnitPrice;
    private BigDecimal baseLineAmount;
    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;
    private List<String> appliedPromotionCodes = new ArrayList<>();
}