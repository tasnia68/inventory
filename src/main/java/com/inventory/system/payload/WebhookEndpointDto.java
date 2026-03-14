package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WebhookEndpointDto {
    private UUID id;
    private String name;
    private String url;
    private String subscribedEvents;
    private String secretKey;
    private String headersJson;
    private Boolean active;
    private LocalDateTime createdAt;
}