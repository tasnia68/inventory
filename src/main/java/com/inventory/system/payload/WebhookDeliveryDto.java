package com.inventory.system.payload;

import com.inventory.system.common.entity.WebhookDeliveryStatus;
import com.inventory.system.common.entity.WebhookEventType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WebhookDeliveryDto {
    private UUID id;
    private UUID webhookEndpointId;
    private String webhookName;
    private WebhookEventType eventType;
    private WebhookDeliveryStatus status;
    private Integer responseStatus;
    private String responseBody;
    private Integer attemptCount;
    private LocalDateTime deliveredAt;
}