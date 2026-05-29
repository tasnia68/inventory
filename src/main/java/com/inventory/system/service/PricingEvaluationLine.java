package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PricingEvaluationLine {
    private ProductVariant productVariant;
    private UUID productVariantId;
    private BigDecimal quantity;
    private BigDecimal baseUnitPrice;
    private BigDecimal finalUnitPrice;
    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;
    private List<String> appliedPromotionCodes;

    public PricingEvaluationLine() {
        this.appliedPromotionCodes = new ArrayList<>();
    }

    public List<String> getAppliedPromotionCodes() {
        if (appliedPromotionCodes == null) appliedPromotionCodes = new ArrayList<>();
        return appliedPromotionCodes;
    }
}
