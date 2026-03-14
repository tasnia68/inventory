package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "webhook_endpoints")
@Getter
@Setter
public class WebhookEndpoint extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(name = "subscribed_events", nullable = false)
    private String subscribedEvents;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}