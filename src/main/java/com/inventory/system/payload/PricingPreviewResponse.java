package com.inventory.system.payload;

import java.math.BigDecimal;
import java.util.List;

public record PricingPreviewResponse(
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal shippingAmount,
        BigDecimal shippingDiscount,
        BigDecimal giftCardAmount,
        BigDecimal grandTotal,
        List<PricingPreviewLine> lines,
        List<AppliedDiscountDto> applied,
        List<String> rejectedCodes,
        List<String> warnings
) {}
