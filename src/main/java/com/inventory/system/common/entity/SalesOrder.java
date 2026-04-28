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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales_orders")
@Getter
@Setter
public class SalesOrder extends BaseEntity {

    @Column(name = "so_number", nullable = false, unique = true)
    private String soNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private OrderPriority priority = OrderPriority.MEDIUM;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "subtotal_amount", precision = 19, scale = 6)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 6)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", precision = 19, scale = 6)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 6)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel")
    private SalesChannel salesChannel = SalesChannel.SALES_ORDER;

    @Column(name = "applied_coupon_codes", columnDefinition = "TEXT")
    private String appliedCouponCodes;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source", length = 32)
    private ExternalOrderSource externalSource;

    @Column(name = "external_order_id", length = 128)
    private String externalOrderId;

    @Column(name = "external_order_ref", length = 256)
    private String externalOrderRef;

    @Column(name = "cod_amount", precision = 19, scale = 6)
    private BigDecimal codAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_zone", length = 32)
    private DeliveryZone deliveryZone;

    @Column(name = "courier_profile_id")
    private UUID courierProfileId;

    @Column(name = "packaging_completed_at")
    private LocalDateTime packagingCompletedAt;

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;
}
