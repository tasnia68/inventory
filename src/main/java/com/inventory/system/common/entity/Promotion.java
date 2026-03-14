package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
public class Promotion extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status = PromotionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private PromotionDiscountType discountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private PromotionScope scope = PromotionScope.ORDER;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel")
    private SalesChannel salesChannel;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    @Column(name = "coupon_required", nullable = false)
    private Boolean couponRequired = false;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "exclusion_group")
    private String exclusionGroup;

    @Column(name = "discount_value", precision = 19, scale = 6)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 19, scale = 6)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 19, scale = 6)
    private BigDecimal minOrderAmount;

    @Column(name = "min_quantity", precision = 19, scale = 6)
    private BigDecimal minQuantity;

    @Column(name = "bundle_quantity", precision = 19, scale = 6)
    private BigDecimal bundleQuantity;

    @Column(name = "bundle_price", precision = 19, scale = 6)
    private BigDecimal bundlePrice;

    @Column(name = "buy_quantity", precision = 19, scale = 6)
    private BigDecimal buyQuantity;

    @Column(name = "get_quantity", precision = 19, scale = 6)
    private BigDecimal getQuantity;

    @Column(name = "usage_limit_total")
    private Integer usageLimitTotal;

    @Column(name = "usage_limit_per_customer")
    private Integer usageLimitPerCustomer;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_category")
    private CustomerCategory customerCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id")
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;
}