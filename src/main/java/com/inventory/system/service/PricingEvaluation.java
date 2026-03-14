package com.inventory.system.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PricingEvaluation {
    private BigDecimal baseSubtotal = BigDecimal.ZERO;
    private BigDecimal lineDiscountTotal = BigDecimal.ZERO;
    private BigDecimal orderDiscountTotal = BigDecimal.ZERO;
    private BigDecimal manualDiscountTotal = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal netSubtotal = BigDecimal.ZERO;
    private List<String> appliedCouponCodes = new ArrayList<>();
    private List<AppliedPromotionEvaluation> appliedPromotions = new ArrayList<>();
    private List<PricingEvaluationLine> lines = new ArrayList<>();
}