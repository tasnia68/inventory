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
@Table(name = "pos_shifts")
@Getter
@Setter
public class PosShift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosShiftStatus status = PosShiftStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_float", precision = 19, scale = 6)
    private BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "expected_cash_amount", precision = 19, scale = 6)
    private BigDecimal expectedCashAmount = BigDecimal.ZERO;

    @Column(name = "declared_cash_amount", precision = 19, scale = 6)
    private BigDecimal declaredCashAmount = BigDecimal.ZERO;

    @Column(name = "over_short_amount", precision = 19, scale = 6)
    private BigDecimal overShortAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_approval_status", nullable = false)
    private PosSettlementApprovalStatus settlementApprovalStatus = PosSettlementApprovalStatus.NOT_REQUIRED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_approved_by")
    private User settlementApprovedBy;

    @Column(name = "settlement_approved_at")
    private LocalDateTime settlementApprovedAt;

    @Column(name = "settlement_approval_notes", columnDefinition = "TEXT")
    private String settlementApprovalNotes;

    @Column(name = "closing_notes", columnDefinition = "TEXT")
    private String closingNotes;
}