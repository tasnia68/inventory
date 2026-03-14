package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pos_suspended_sales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"suspended_number", "tenant_id"})
})
@Getter
@Setter
public class PosSuspendedSale extends BaseEntity {

    @Column(name = "suspended_number", nullable = false)
    private String suspendedNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_sale_id")
    private PosSale completedSale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosSuspendedSaleStatus status = PosSuspendedSaleStatus.SUSPENDED;

    @Column(name = "suspended_at", nullable = false)
    private LocalDateTime suspendedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "manual_discount_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency;

    @Column(name = "coupon_codes", columnDefinition = "TEXT")
    private String couponCodes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "suspendedSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PosSuspendedSaleItem> items = new ArrayList<>();
}