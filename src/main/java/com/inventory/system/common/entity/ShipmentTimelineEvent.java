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

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_timeline_events")
@Getter
@Setter
public class ShipmentTimelineEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ShipmentTimelineEventType eventType;

    @Column(name = "event_source", length = 100)
    private String eventSource;

    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status")
    private ShipmentStatus shipmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_dispatch_status")
    private CourierDispatchStatus courierDispatchStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_review_status")
    private DeliveryReviewStatus deliveryReviewStatus;

    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible = true;
}