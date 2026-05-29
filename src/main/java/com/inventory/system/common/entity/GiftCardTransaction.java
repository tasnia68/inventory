package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gift_card_transactions")
@Getter
@Setter
public class GiftCardTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_card_id", nullable = false)
    private GiftCard giftCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GiftCardTransactionType type;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal balanceAfter;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "pos_sale_id")
    private UUID posSaleId;

    @Column(length = 255)
    private String reference;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
