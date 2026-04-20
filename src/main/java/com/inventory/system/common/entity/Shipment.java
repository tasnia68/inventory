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
@Table(name = "shipments")
@Getter
@Setter
public class Shipment extends BaseEntity {

    @Column(name = "shipment_number", nullable = false, unique = true)
    private String shipmentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status = ShipmentStatus.DRAFT;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "courier_provider")
    private String courierProvider;

    @Column(name = "courier_service")
    private String courierService;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_dispatch_status", nullable = false)
    private CourierDispatchStatus courierDispatchStatus = CourierDispatchStatus.UNASSIGNED;

    @Column(name = "courier_reference")
    private String courierReference;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    @Column(name = "shipping_label_url", columnDefinition = "TEXT")
    private String shippingLabelUrl;

    @Column(name = "cash_on_delivery_amount", precision = 19, scale = 6)
    private BigDecimal cashOnDeliveryAmount;

    @Column(name = "delivery_fee", precision = 19, scale = 6)
    private BigDecimal deliveryFee;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "pickup_requested_at")
    private LocalDateTime pickupRequestedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "last_courier_event", columnDefinition = "TEXT")
    private String lastCourierEvent;

    @Column(name = "last_courier_sync_at")
    private LocalDateTime lastCourierSyncAt;

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentItem> items = new ArrayList<>();
}
