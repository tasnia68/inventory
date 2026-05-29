package com.inventory.system.payload;

import com.inventory.system.common.entity.ReferralCodeStatus;

import java.util.UUID;

public record ReferralCodeDto(
        UUID id,
        UUID programId,
        UUID customerId,
        String code,
        ReferralCodeStatus status,
        Integer refereesCount,
        Integer rewardsPaidCount
) {}
