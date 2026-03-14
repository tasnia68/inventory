package com.inventory.system.service;

import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.payload.WebhookDeliveryDto;
import com.inventory.system.payload.WebhookEndpointDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WebhookService {
    List<WebhookEndpointDto> getEndpoints();

    WebhookEndpointDto getEndpoint(UUID id);

    WebhookEndpointDto createEndpoint(WebhookEndpointDto request);

    WebhookEndpointDto updateEndpoint(UUID id, WebhookEndpointDto request);

    void deleteEndpoint(UUID id);

    List<WebhookDeliveryDto> getDeliveries();

    void publishEvent(WebhookEventType eventType, Map<String, Object> payload);
}