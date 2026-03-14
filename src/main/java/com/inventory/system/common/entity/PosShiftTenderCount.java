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

@Entity
@Table(name = "pos_shift_tender_counts")
@Getter
@Setter
public class PosShiftTenderCount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private PosShift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PosPaymentMethod paymentMethod;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "declared_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal declaredAmount = BigDecimal.ZERO;

    @Column(name = "variance_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal varianceAmount = BigDecimal.ZERO;
}