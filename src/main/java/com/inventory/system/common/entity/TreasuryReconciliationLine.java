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
import java.time.LocalDate;

@Entity
@Table(name = "treasury_reconciliation_lines")
@Getter
@Setter
public class TreasuryReconciliationLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private TreasuryReconciliation reconciliation;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ReconciliationSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "source_reference", nullable = false)
    private String sourceReference;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "matched", nullable = false)
    private boolean matched = true;
}
