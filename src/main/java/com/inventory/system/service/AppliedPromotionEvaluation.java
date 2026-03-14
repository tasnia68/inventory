package com.inventory.system.service;

import com.inventory.system.common.entity.Coupon;
import com.inventory.system.common.entity.Promotion;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AppliedPromotionEvaluation {
    private Promotion promotion;
    private Coupon coupon;
    private BigDecimal discountAmount = BigDecimal.ZERO;
}