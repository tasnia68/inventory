package com.inventory.system.payload;

import com.inventory.system.common.entity.GiftCardSource;
import com.inventory.system.common.entity.GiftCardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GiftCardDto(
        UUID id,
        String code,
        GiftCardStatus status,
        String currency,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        UUID issuedToCustomerId,
        String issuedToCustomerName,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        GiftCardSource source,
        String notes
) {}
