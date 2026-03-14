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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_refunds")
@Getter
@Setter
public class SalesRefund extends BaseEntity {

    @Column(name = "refund_number", nullable = false, unique = true)
    private String refundNumber;

    @Column(name = "credit_note_number", unique = true)
    private String creditNoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rma_id")
    private ReturnMerchandiseAuthorization rma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_sale_id")
    private PosSale posSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_sales_order_id")
    private SalesOrder replacementSalesOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesRefundStatus status = SalesRefundStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private SalesRefundType refundType = SalesRefundType.REFUND;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false)
    private RefundMethod refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_payment_method")
    private PosPaymentMethod originalPaymentMethod;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "replacement_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal replacementAmount = BigDecimal.ZERO;

    @Column(name = "net_refund_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal netRefundAmount = BigDecimal.ZERO;

    @Column(name = "amount_due_from_customer", nullable = false, precision = 19, scale = 6)
    private BigDecimal amountDueFromCustomer = BigDecimal.ZERO;

    @Column(name = "store_credit_issued", nullable = false, precision = 19, scale = 6)
    private BigDecimal storeCreditIssued = BigDecimal.ZERO;

    @Column(name = "exchange_price_difference", nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangePriceDifference = BigDecimal.ZERO;

    @Column(name = "document_generated_at")
    private LocalDateTime documentGeneratedAt;

    @Column(name = "document_content", columnDefinition = "TEXT")
    private String documentContent;

    @OneToMany(mappedBy = "salesRefund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesRefundItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "salesRefund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesRefundAuditEntry> auditEntries = new ArrayList<>();
}