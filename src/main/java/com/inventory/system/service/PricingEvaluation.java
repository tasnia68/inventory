package com.inventory.system.service;

import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.DiscountCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class PricingEvaluation {
    private List<PricingEvaluationLine> lines = new ArrayList<>();
    private Set<String> appliedCouponCodes = new LinkedHashSet<>();
    private List<AppliedDiscount> appliedDiscounts = new ArrayList<>();
    private BigDecimal baseSubtotal = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal netSubtotal = BigDecimal.ZERO;
    private BigDecimal shippingDiscount = BigDecimal.ZERO;
    /** Total amount that gift cards would absorb against (netSubtotal + shipping). Read-only preview. */
    private BigDecimal giftCardAmount = BigDecimal.ZERO;
    private Set<String> appliedGiftCardCodes = new LinkedHashSet<>();
    private List<String> warnings = new ArrayList<>();

    @Getter
    @Setter
    public static class AppliedDiscount {
        private Discount discount;
        private DiscountCode discountCode;
        private BigDecimal amount;
        private String message;

        public AppliedDiscount(Discount discount, DiscountCode discountCode, BigDecimal amount) {
            this.discount = discount;
            this.discountCode = discountCode;
            this.amount = amount;
        }
    }
}
