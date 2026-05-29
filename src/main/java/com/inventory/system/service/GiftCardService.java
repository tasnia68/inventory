package com.inventory.system.service;

import com.inventory.system.payload.AdjustGiftCardRequest;
import com.inventory.system.payload.GiftCardBalanceDto;
import com.inventory.system.payload.GiftCardDto;
import com.inventory.system.payload.GiftCardTransactionDto;
import com.inventory.system.payload.IssueGiftCardRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GiftCardService {

    GiftCardDto issue(IssueGiftCardRequest request);

    List<GiftCardDto> list();

    GiftCardDto get(UUID id);

    GiftCardBalanceDto getBalance(String code);

    GiftCardDto adjust(UUID id, AdjustGiftCardRequest request);

    BigDecimal redeem(String code, BigDecimal amount, UUID salesOrderId, UUID posSaleId, String reference);

    /**
     * Read-only: for each code in order, computes how much of {@code dueAmount} it would absorb.
     * Returns ordered map of normalized-code -> redeemable amount. Skips invalid/inactive cards silently.
     */
    java.util.LinkedHashMap<String, BigDecimal> previewRedemption(List<String> codes, BigDecimal dueAmount);

    /**
     * Mutating: actually debits each code's balance against dueAmount in order. Returns total redeemed.
     */
    BigDecimal redeemCodes(List<String> codes, BigDecimal dueAmount, UUID salesOrderId, UUID posSaleId, String reference);

    /**
     * Reverses every REDEEM transaction for the given sales order by creating REVERSAL transactions
     * that add the original amount back to each card. Reactivates REDEEMED cards if their balance
     * becomes positive again. Returns the total amount refunded across all cards.
     */
    BigDecimal reverseRedemptionsForOrder(UUID salesOrderId, String reference);

    List<GiftCardTransactionDto> transactions(UUID giftCardId);
}
