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

@Entity
@Table(name = "pos_refund_settlement_impacts")
@Getter
@Setter
public class PosRefundSettlementImpact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_refund_id", nullable = false)
    private SalesRefund salesRefund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private PosShift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id")
    private PosTerminal terminal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PosPaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;
}