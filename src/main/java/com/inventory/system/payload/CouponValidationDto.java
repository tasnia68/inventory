package com.inventory.system.payload;

import lombok.Data;

@Data
public class CouponValidationDto {
    private boolean valid;
    private String message;
    private CouponDto coupon;
    private PricingPreviewDto preview;
}