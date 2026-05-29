package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountCodeStatus;

import java.time.LocalDateTime;

public record CreateDiscountCodeRequest(
        String code,
        DiscountCodeStatus status,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer maxRedemptions,
        Integer maxRedemptionsPerCustomer,
        String notes
) {}
