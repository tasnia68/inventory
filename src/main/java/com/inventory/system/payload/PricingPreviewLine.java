package com.inventory.system.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PricingPreviewLine(
        UUID productVariantId,
        BigDecimal quantity,
        BigDecimal baseUnitPrice,
        BigDecimal lineSubtotal,
        BigDecimal lineDiscount,
        BigDecimal lineTotal,
        List<String> appliedDiscountCodes
) {}
