package com.inventory.system.payload;

import com.inventory.system.common.entity.GiftCardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GiftCardBalanceDto(
        String code,
        GiftCardStatus status,
        String currency,
        BigDecimal currentBalance,
        LocalDateTime expiresAt
) {}
