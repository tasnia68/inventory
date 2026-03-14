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
@Table(name = "pricing_rules", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
public class PricingRule extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingRuleStatus status = PricingRuleStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private PricingRuleAdjustmentType adjustmentType;

    @Column(name = "adjustment_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal adjustmentValue;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel")
    private SalesChannel salesChannel;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "min_quantity", precision = 19, scale = 6)
    private BigDecimal minQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_category")
    private CustomerCategory customerCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

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

    @Column(columnDefinition = "TEXT")
    private String notes;
}