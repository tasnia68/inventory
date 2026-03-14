package com.inventory.system.payload;

import com.inventory.system.common.entity.PromotionDiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AppliedPromotionDto {
    private UUID promotionId;
    private String promotionName;
    private String promotionCode;
    private PromotionDiscountType discountType;
    private UUID couponId;
    private String couponCode;
    private BigDecimal discountAmount;
    private Boolean stackable;
}