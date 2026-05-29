package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountKind;

import java.math.BigDecimal;
import java.util.UUID;

public record AppliedDiscountDto(
        UUID discountId,
        UUID discountCodeId,
        String code,
        String name,
        DiscountKind kind,
        BigDecimal discountAmount,
        String message
) {}
