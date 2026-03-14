package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PricingEvaluationLine {
    private ProductVariant productVariant;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal baseUnitPrice = BigDecimal.ZERO;
    private BigDecimal finalUnitPrice = BigDecimal.ZERO;
    private BigDecimal baseLineAmount = BigDecimal.ZERO;
    private BigDecimal lineDiscountAmount = BigDecimal.ZERO;
    private BigDecimal lineTotalAmount = BigDecimal.ZERO;
    private BigDecimal manualLineDiscount = BigDecimal.ZERO;
    private List<String> appliedPromotionCodes = new ArrayList<>();
}