package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountValueType;

import java.math.BigDecimal;
import java.util.UUID;

public record DiscountTierDto(
        UUID id,
        BigDecimal minSubtotal,
        BigDecimal minQuantity,
        DiscountValueType valueType,
        BigDecimal value,
        Integer sortOrder
) {}
