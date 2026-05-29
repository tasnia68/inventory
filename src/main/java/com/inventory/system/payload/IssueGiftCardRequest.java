package com.inventory.system.payload;

import com.inventory.system.common.entity.GiftCardSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record IssueGiftCardRequest(
        String code,
        String currency,
        BigDecimal initialBalance,
        UUID issuedToCustomerId,
        LocalDateTime expiresAt,
        GiftCardSource source,
        String notes
) {}
