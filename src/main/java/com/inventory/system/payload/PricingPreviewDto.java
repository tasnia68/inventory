package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PricingPreviewDto {
    private BigDecimal baseSubtotal;
    private BigDecimal lineDiscountTotal;
    private BigDecimal orderDiscountTotal;
    private BigDecimal manualDiscountTotal;
    private BigDecimal totalDiscount;
    private BigDecimal netSubtotal;
    private List<String> appliedCouponCodes = new ArrayList<>();
    private List<AppliedPromotionDto> appliedPromotions = new ArrayList<>();
    private List<PricingPreviewLineDto> lines = new ArrayList<>();
}