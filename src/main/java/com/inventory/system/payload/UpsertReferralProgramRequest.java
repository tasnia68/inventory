package com.inventory.system.payload;

import com.inventory.system.common.entity.ReferralProgramStatus;
import com.inventory.system.common.entity.ReferralRewardTrigger;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertReferralProgramRequest(
        String name,
        ReferralProgramStatus status,
        UUID referrerDiscountId,
        UUID refereeDiscountId,
        ReferralRewardTrigger rewardTrigger,
        BigDecimal minRefereeOrderAmount,
        Integer refereeNthOrder,
        Integer maxReferralsPerCustomer,
        String description
) {}
