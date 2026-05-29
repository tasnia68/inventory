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
@Table(name = "discount_redemptions")
@Getter
@Setter
public class DiscountRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @Column(name = "pos_sale_id")
    private java.util.UUID posSaleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false, length = 32)
    private SalesChannel salesChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiscountRedemptionStatus status = DiscountRedemptionStatus.APPLIED;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal discountAmount;

    @Column(name = "order_subtotal", precision = 19, scale = 6)
    private BigDecimal orderSubtotal;

    @Column(name = "abuse_flag", nullable = false)
    private Boolean abuseFlag = false;

    @Column(name = "abuse_reason", columnDefinition = "TEXT")
    private String abuseReason;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;
}
