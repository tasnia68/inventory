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
@Table(name = "customer_store_credit_transactions")
@Getter
@Setter
public class CustomerStoreCreditTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_refund_id")
    private SalesRefund salesRefund;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StoreCreditTransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 6)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal balanceAfter;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
}