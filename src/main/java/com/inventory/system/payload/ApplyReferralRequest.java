package com.inventory.system.payload;

import java.util.UUID;

public record ApplyReferralRequest(
        String referralCode,
        UUID refereeCustomerId
) {}
