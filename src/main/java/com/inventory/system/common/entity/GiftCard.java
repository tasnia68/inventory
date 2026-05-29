package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gift_cards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
public class GiftCard extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GiftCardStatus status = GiftCardStatus.ACTIVE;

    @Column(nullable = false, length = 8)
    private String currency = "BDT";

    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 6)
    private BigDecimal initialBalance;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 6)
    private BigDecimal currentBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_to_customer_id")
    private Customer issuedToCustomer;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GiftCardSource source = GiftCardSource.MANUAL;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
