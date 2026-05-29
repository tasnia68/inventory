package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PricingPreviewRequest(
        UUID customerId,
        String customerEmail,
        DiscountChannel channel,
        BigDecimal shippingAmount,
        String shippingCountry,
        LocalDateTime evaluationTime,
        List<String> discountCodes,
        List<String> giftCardCodes,
        String referralCode,
        List<PricingPreviewItemRequest> items
) {}
