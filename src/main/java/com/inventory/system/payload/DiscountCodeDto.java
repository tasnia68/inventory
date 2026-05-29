package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountCodeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiscountCodeDto(
        UUID id,
        UUID discountId,
        String code,
        DiscountCodeStatus status,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer maxRedemptions,
        Integer maxRedemptionsPerCustomer,
        Integer redeemedCount,
        String notes
) {}
