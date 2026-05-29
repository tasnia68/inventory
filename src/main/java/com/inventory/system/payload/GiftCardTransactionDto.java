package com.inventory.system.payload;

import com.inventory.system.common.entity.GiftCardTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GiftCardTransactionDto(
        UUID id,
        UUID giftCardId,
        GiftCardTransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        UUID salesOrderId,
        UUID posSaleId,
        String reference,
        LocalDateTime occurredAt
) {}
