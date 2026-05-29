package com.inventory.system.payload;

import com.inventory.system.common.entity.ReferralAttributionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReferralAttributionDto(
        UUID id,
        UUID referralCodeId,
        UUID refereeCustomerId,
        UUID refereeOrderId,
        ReferralAttributionStatus status,
        LocalDateTime qualifiedAt,
        LocalDateTime rewardedAt,
        BigDecimal referrerRewardAmount,
        BigDecimal refereeRewardAmount,
        String notes
) {}
