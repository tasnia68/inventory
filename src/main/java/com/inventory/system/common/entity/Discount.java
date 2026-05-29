package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discounts")
@Getter
@Setter
public class Discount extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiscountStatus status = DiscountStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private DiscountKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 32)
    private DiscountValueType valueType;

    @Column(precision = 19, scale = 6)
    private BigDecimal value;

    @Column(name = "max_discount_amount", precision = 19, scale = 6)
    private BigDecimal maxDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to_scope", nullable = false, length = 32)
    private DiscountAppliesToScope appliesToScope = DiscountAppliesToScope.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_eligibility", nullable = false, length = 48)
    private DiscountCustomerEligibility customerEligibility = DiscountCustomerEligibility.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_purchase_type", nullable = false, length = 32)
    private DiscountMinPurchaseType minPurchaseType = DiscountMinPurchaseType.NONE;

    @Column(name = "min_purchase_amount", precision = 19, scale = 6)
    private BigDecimal minPurchaseAmount;

    @Column(name = "min_purchase_quantity", precision = 19, scale = 6)
    private BigDecimal minPurchaseQuantity;

    @Column(name = "usage_limit_total")
    private Integer usageLimitTotal;

    @Column(name = "usage_limit_per_customer")
    private Integer usageLimitPerCustomer;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "schedule_days_of_week", length = 64)
    private String scheduleDaysOfWeek;

    @Column(name = "schedule_start_time")
    private LocalTime scheduleStartTime;

    @Column(name = "schedule_end_time")
    private LocalTime scheduleEndTime;

    @Column(name = "schedule_timezone", length = 64)
    private String scheduleTimezone;

    @Column(nullable = false)
    private Boolean stackable = false;

    @Column(name = "combine_with_order_discounts", nullable = false)
    private Boolean combineWithOrderDiscounts = false;

    @Column(name = "combine_with_product_discounts", nullable = false)
    private Boolean combineWithProductDiscounts = false;

    @Column(name = "combine_with_shipping_discounts", nullable = false)
    private Boolean combineWithShippingDiscounts = true;

    @Column(name = "exclusion_group", length = 64)
    private String exclusionGroup;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(name = "auto_apply", nullable = false)
    private Boolean autoApply = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false, length = 32)
    private DiscountChannel salesChannel = DiscountChannel.ALL;

    @Column(name = "bogo_buy_quantity", precision = 19, scale = 6)
    private BigDecimal bogoBuyQuantity;

    @Column(name = "bogo_get_quantity", precision = 19, scale = 6)
    private BigDecimal bogoGetQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "bogo_get_value_type", length = 32)
    private DiscountValueType bogoGetValueType;

    @Column(name = "bogo_get_value", precision = 19, scale = 6)
    private BigDecimal bogoGetValue;

    @Column(name = "bundle_quantity", precision = 19, scale = 6)
    private BigDecimal bundleQuantity;

    @Column(name = "bundle_price", precision = 19, scale = 6)
    private BigDecimal bundlePrice;

    @Column(name = "free_shipping_max_amount", precision = 19, scale = 6)
    private BigDecimal freeShippingMaxAmount;

    @Column(name = "free_shipping_countries", columnDefinition = "TEXT")
    private String freeShippingCountries;

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscountTier> tiers = new ArrayList<>();

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscountProductInclusion> productInclusions = new ArrayList<>();

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscountCustomerInclusion> customerInclusions = new ArrayList<>();
}
