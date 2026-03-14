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
@Table(name = "pos_sale_payments")
@Getter
@Setter
public class PosSalePayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private PosSale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PosPaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;
}