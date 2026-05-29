package com.inventory.system.service.impl;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.GiftCard;
import com.inventory.system.common.entity.GiftCardSource;
import com.inventory.system.common.entity.GiftCardStatus;
import com.inventory.system.common.entity.GiftCardTransaction;
import com.inventory.system.common.entity.GiftCardTransactionType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AdjustGiftCardRequest;
import com.inventory.system.payload.GiftCardBalanceDto;
import com.inventory.system.payload.GiftCardDto;
import com.inventory.system.payload.GiftCardTransactionDto;
import com.inventory.system.payload.IssueGiftCardRequest;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.GiftCardRepository;
import com.inventory.system.repository.GiftCardTransactionRepository;
import com.inventory.system.service.GiftCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GiftCardServiceImpl implements GiftCardService {

    private static final int SCALE = 6;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final GiftCardRepository giftCardRepository;
    private final GiftCardTransactionRepository txRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public GiftCardDto issue(IssueGiftCardRequest request) {
        if (request.initialBalance() == null || request.initialBalance().signum() <= 0) {
            throw new BadRequestException("Initial balance must be positive");
        }
        String code = (request.code() != null && !request.code().isBlank())
                ? request.code().trim().toUpperCase()
                : generateCode();
        if (giftCardRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BadRequestException("Gift card code already exists: " + code);
        }

        Customer customer = null;
        if (request.issuedToCustomerId() != null) {
            customer = customerRepository.findById(request.issuedToCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        GiftCard card = new GiftCard();
        card.setCode(code);
        card.setStatus(GiftCardStatus.ACTIVE);
        card.setCurrency(request.currency() != null && !request.currency().isBlank() ? request.currency() : "BDT");
        card.setInitialBalance(request.initialBalance().setScale(SCALE, RM));
        card.setCurrentBalance(card.getInitialBalance());
        card.setIssuedToCustomer(customer);
        card.setIssuedAt(LocalDateTime.now());
        card.setExpiresAt(request.expiresAt());
        card.setSource(request.source() != null ? request.source() : GiftCardSource.MANUAL);
        card.setNotes(request.notes());
        card = giftCardRepository.save(card);

        recordTransaction(card, GiftCardTransactionType.ISSUE, card.getInitialBalance(), null, null,
                "Initial issuance");
        return toDto(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftCardDto> list() {
        return giftCardRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GiftCardDto get(UUID id) {
        GiftCard card = giftCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gift card not found"));
        return toDto(card);
    }

    @Override
    @Transactional(readOnly = true)
    public GiftCardBalanceDto getBalance(String code) {
        GiftCard card = giftCardRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Gift card not found"));
        return new GiftCardBalanceDto(card.getCode(), card.getStatus(), card.getCurrency(),
                card.getCurrentBalance(), card.getExpiresAt());
    }

    @Override
    @Transactional
    public GiftCardDto adjust(UUID id, AdjustGiftCardRequest request) {
        GiftCard card = giftCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gift card not found"));
        BigDecimal amount = request.amount();
        if (amount == null) throw new BadRequestException("Adjustment amount required");

        BigDecimal newBalance = card.getCurrentBalance();
        BigDecimal signed = amount;
        switch (request.type()) {
            case REFUND, ISSUE, ADJUSTMENT, REVERSAL -> {
                signed = amount.abs();
                newBalance = newBalance.add(signed);
            }
            case REDEEM, EXPIRE -> {
                signed = amount.abs().negate();
                newBalance = newBalance.add(signed);
                if (newBalance.signum() < 0) {
                    throw new BadRequestException("Adjustment would drive balance below zero");
                }
            }
        }
        card.setCurrentBalance(newBalance.setScale(SCALE, RM));
        if (card.getCurrentBalance().signum() == 0 && request.type() == GiftCardTransactionType.REDEEM) {
            card.setStatus(GiftCardStatus.REDEEMED);
        }
        giftCardRepository.save(card);
        recordTransaction(card, request.type(), signed, null, null, request.reference());
        return toDto(card);
    }

    @Override
    @Transactional
    public BigDecimal redeem(String code, BigDecimal amount, UUID salesOrderId, UUID posSaleId, String reference) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        GiftCard card = giftCardRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Gift card not found: " + code));
        if (card.getStatus() != GiftCardStatus.ACTIVE) {
            throw new BadRequestException("Gift card is not active");
        }
        if (card.getExpiresAt() != null && LocalDateTime.now().isAfter(card.getExpiresAt())) {
            card.setStatus(GiftCardStatus.EXPIRED);
            giftCardRepository.save(card);
            throw new BadRequestException("Gift card expired");
        }
        BigDecimal redeemed = amount.min(card.getCurrentBalance());
        card.setCurrentBalance(card.getCurrentBalance().subtract(redeemed).setScale(SCALE, RM));
        card.setLastUsedAt(LocalDateTime.now());
        if (card.getCurrentBalance().signum() == 0) card.setStatus(GiftCardStatus.REDEEMED);
        giftCardRepository.save(card);
        recordTransaction(card, GiftCardTransactionType.REDEEM, redeemed.negate(), salesOrderId, posSaleId, reference);
        return redeemed;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.LinkedHashMap<String, BigDecimal> previewRedemption(List<String> codes, BigDecimal dueAmount) {
        java.util.LinkedHashMap<String, BigDecimal> out = new java.util.LinkedHashMap<>();
        if (codes == null || dueAmount == null || dueAmount.signum() <= 0) return out;
        BigDecimal remaining = dueAmount;
        LocalDateTime now = LocalDateTime.now();
        for (String raw : codes) {
            if (raw == null || raw.isBlank() || remaining.signum() <= 0) continue;
            String code = raw.trim();
            if (out.containsKey(code)) continue;
            var opt = giftCardRepository.findByCodeIgnoreCase(code);
            if (opt.isEmpty()) continue;
            GiftCard card = opt.get();
            if (card.getStatus() != GiftCardStatus.ACTIVE) continue;
            if (card.getExpiresAt() != null && now.isAfter(card.getExpiresAt())) continue;
            if (card.getCurrentBalance() == null || card.getCurrentBalance().signum() <= 0) continue;
            BigDecimal applied = remaining.min(card.getCurrentBalance()).setScale(SCALE, RM);
            if (applied.signum() <= 0) continue;
            out.put(code, applied);
            remaining = remaining.subtract(applied);
        }
        return out;
    }

    @Override
    @Transactional
    public BigDecimal redeemCodes(List<String> codes, BigDecimal dueAmount, UUID salesOrderId, UUID posSaleId, String reference) {
        if (codes == null || dueAmount == null || dueAmount.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal remaining = dueAmount;
        BigDecimal total = BigDecimal.ZERO;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String raw : codes) {
            if (raw == null || raw.isBlank() || remaining.signum() <= 0) continue;
            String code = raw.trim();
            if (!seen.add(code)) continue;
            try {
                BigDecimal applied = redeem(code, remaining, salesOrderId, posSaleId, reference);
                if (applied != null && applied.signum() > 0) {
                    remaining = remaining.subtract(applied);
                    total = total.add(applied);
                }
            } catch (ResourceNotFoundException | BadRequestException ignored) {
                // Skip invalid/inactive cards rather than aborting the whole order
            }
        }
        return total.setScale(SCALE, RM);
    }

    @Override
    @Transactional
    public BigDecimal reverseRedemptionsForOrder(UUID salesOrderId, String reference) {
        if (salesOrderId == null) return BigDecimal.ZERO;
        List<GiftCardTransaction> originalRedeems = txRepository
                .findBySalesOrderIdAndType(salesOrderId, GiftCardTransactionType.REDEEM);
        BigDecimal total = BigDecimal.ZERO;
        for (GiftCardTransaction redeem : originalRedeems) {
            BigDecimal originalAmount = redeem.getAmount();
            if (originalAmount == null || originalAmount.signum() >= 0) continue; // redemptions are negative
            BigDecimal refund = originalAmount.abs();
            GiftCard card = redeem.getGiftCard();
            card.setCurrentBalance(card.getCurrentBalance().add(refund).setScale(SCALE, RM));
            if (card.getStatus() == GiftCardStatus.REDEEMED) {
                card.setStatus(GiftCardStatus.ACTIVE);
            }
            giftCardRepository.save(card);
            recordTransaction(card, GiftCardTransactionType.REVERSAL, refund, salesOrderId, redeem.getPosSaleId(),
                    reference != null ? reference : "Order updated");
            total = total.add(refund);
        }
        return total.setScale(SCALE, RM);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftCardTransactionDto> transactions(UUID giftCardId) {
        return txRepository.findByGiftCardIdOrderByOccurredAtDesc(giftCardId).stream()
                .map(this::toTxDto).toList();
    }

    // ---------------- helpers ----------------

    private void recordTransaction(GiftCard card, GiftCardTransactionType type, BigDecimal amount,
                                   UUID salesOrderId, UUID posSaleId, String reference) {
        GiftCardTransaction tx = new GiftCardTransaction();
        tx.setGiftCard(card);
        tx.setType(type);
        tx.setAmount(amount.setScale(SCALE, RM));
        tx.setBalanceAfter(card.getCurrentBalance());
        tx.setSalesOrderId(salesOrderId);
        tx.setPosSaleId(posSaleId);
        tx.setReference(reference);
        tx.setOccurredAt(LocalDateTime.now());
        txRepository.save(tx);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("GC-");
        for (int i = 0; i < 12; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }

    private GiftCardDto toDto(GiftCard card) {
        return new GiftCardDto(
                card.getId(),
                card.getCode(),
                card.getStatus(),
                card.getCurrency(),
                card.getInitialBalance(),
                card.getCurrentBalance(),
                card.getIssuedToCustomer() != null ? card.getIssuedToCustomer().getId() : null,
                card.getIssuedToCustomer() != null ? card.getIssuedToCustomer().getName() : null,
                card.getIssuedAt(),
                card.getExpiresAt(),
                card.getLastUsedAt(),
                card.getSource(),
                card.getNotes()
        );
    }

    private GiftCardTransactionDto toTxDto(GiftCardTransaction tx) {
        return new GiftCardTransactionDto(
                tx.getId(),
                tx.getGiftCard().getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getSalesOrderId(),
                tx.getPosSaleId(),
                tx.getReference(),
                tx.getOccurredAt()
        );
    }
}
