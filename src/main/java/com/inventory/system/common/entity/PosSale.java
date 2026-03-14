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
@Table(name = "pos_sales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"receipt_number", "tenant_id"}),
        @UniqueConstraint(columnNames = {"client_sale_id", "tenant_id"})
})
@Getter
@Setter
public class PosSale extends BaseEntity {

    @Column(name = "receipt_number", nullable = false)
    private String receiptNumber;

    @Column(name = "client_sale_id")
    private String clientSaleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private PosShift shift;

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
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transaction_id")
    private StockTransaction stockTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false)
    private PosSaleStatus saleStatus = PosSaleStatus.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private PosSyncStatus syncStatus = PosSyncStatus.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PosPaymentMethod paymentMethod;

    @Column(name = "sale_time", nullable = false)
    private LocalDateTime saleTime;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAmount;

    @Column(name = "tendered_amount", precision = 19, scale = 6)
    private BigDecimal tenderedAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 19, scale = 6)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency;

    @Column(name = "applied_coupon_codes", columnDefinition = "TEXT")
    private String appliedCouponCodes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_sale_id")
    private PosSuspendedSale suspendedSale;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PosSaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PosSalePayment> payments = new ArrayList<>();
}