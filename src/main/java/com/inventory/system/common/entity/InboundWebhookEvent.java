package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbound_webhook_events")
@Getter
@Setter
public class InboundWebhookEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private ExternalOrderSource source;

    @Column(name = "external_event_id", length = 128)
    private String externalEventId;

    @Column(name = "topic", length = 128)
    private String topic;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "signature", length = 512)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InboundWebhookStatus status = InboundWebhookStatus.RECEIVED;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
