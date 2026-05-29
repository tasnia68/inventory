package com.inventory.system.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingPreviewItemRequest(
        UUID productVariantId,
        UUID categoryId,
        BigDecimal quantity,
        BigDecimal unitPrice
) {}
