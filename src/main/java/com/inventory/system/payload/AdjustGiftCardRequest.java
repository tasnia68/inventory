package com.inventory.system.payload;

import com.inventory.system.common.entity.GiftCardTransactionType;

import java.math.BigDecimal;

public record AdjustGiftCardRequest(
        GiftCardTransactionType type,
        BigDecimal amount,
        String reference
) {}
