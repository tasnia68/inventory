package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.WebhookDelivery;
import com.inventory.system.common.entity.WebhookDeliveryStatus;
import com.inventory.system.common.entity.WebhookEndpoint;
import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.WebhookDeliveryDto;
import com.inventory.system.payload.WebhookEndpointDto;
import com.inventory.system.repository.WebhookDeliveryRepository;
import com.inventory.system.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TaskExecutor reportingTaskExecutor;

    @Override
    @Transactional(readOnly = true)
    public List<WebhookEndpointDto> getEndpoints() {
        return webhookEndpointRepository.findAll().stream().map(this::mapEndpoint).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookEndpointDto getEndpoint(UUID id) {
        return mapEndpoint(getEndpointEntity(id));
    }

    @Override
    @Transactional
    public WebhookEndpointDto createEndpoint(WebhookEndpointDto request) {
        WebhookEndpoint endpoint = new WebhookEndpoint();
        apply(endpoint, request);
        return mapEndpoint(webhookEndpointRepository.save(endpoint));
    }

    @Override
    @Transactional
    public WebhookEndpointDto updateEndpoint(UUID id, WebhookEndpointDto request) {
        WebhookEndpoint endpoint = getEndpointEntity(id);
        apply(endpoint, request);
        return mapEndpoint(webhookEndpointRepository.save(endpoint));
    }

    @Override
    @Transactional
    public void deleteEndpoint(UUID id) {
        webhookEndpointRepository.delete(getEndpointEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryDto> getDeliveries() {
        return webhookDeliveryRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::mapDelivery).toList();
    }

    @Override
    public void publishEvent(WebhookEventType eventType, Map<String, Object> payload) {
        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findByActiveTrue().stream()
                .filter(endpoint -> isSubscribed(endpoint, eventType))
                .toList();
        for (WebhookEndpoint endpoint : endpoints) {
            reportingTaskExecutor.execute(() -> deliverEvent(endpoint.getId(), eventType, payload));
        }
    }

    @Transactional
    protected void deliverEvent(UUID endpointId, WebhookEventType eventType, Map<String, Object> payload) {
        WebhookEndpoint endpoint = getEndpointEntity(endpointId);
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setWebhookEndpoint(endpoint);
        delivery.setEventType(eventType);
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttemptCount(1);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventType", eventType.name());
        envelope.put("timestamp", LocalDateTime.now().toString());
        envelope.put("payload", payload);

        String payloadJson = toJson(envelope);
        delivery.setPayloadJson(payloadJson);
        delivery = webhookDeliveryRepository.save(delivery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        headers.add("X-Webhook-Event", eventType.name());
        if (endpoint.getSecretKey() != null && !endpoint.getSecretKey().isBlank()) {
            headers.add("X-Webhook-Secret", endpoint.getSecretKey());
        }
        headers.putAll(parseHeaders(endpoint.getHeadersJson()));

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint.getUrl(), new HttpEntity<>(payloadJson, headers), String.class);
            delivery.setStatus(WebhookDeliveryStatus.SUCCESS);
            delivery.setResponseStatus(response.getStatusCode().value());
            delivery.setResponseBody(response.getBody());
            delivery.setDeliveredAt(LocalDateTime.now());
        } catch (RestClientException exception) {
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            delivery.setResponseBody(exception.getMessage());
            delivery.setDeliveredAt(LocalDateTime.now());
        }

        webhookDeliveryRepository.save(delivery);
    }

    private boolean isSubscribed(WebhookEndpoint endpoint, WebhookEventType eventType) {
        return Arrays.stream(endpoint.getSubscribedEvents().split(","))
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(eventType.name()));
    }

    private Map<String, List<String>> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(headersJson, STRING_MAP);
            Map<String, List<String>> headers = new LinkedHashMap<>();
            parsed.forEach((key, value) -> headers.put(key, List.of(value)));
            return headers;
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Invalid webhook headers JSON", exception);
        }
    }

    private void apply(WebhookEndpoint endpoint, WebhookEndpointDto request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Webhook name is required");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new BadRequestException("Webhook URL is required");
        }
        if (request.getSubscribedEvents() == null || request.getSubscribedEvents().isBlank()) {
            throw new BadRequestException("Subscribed events are required");
        }
        endpoint.setName(request.getName());
        endpoint.setUrl(request.getUrl());
        endpoint.setSubscribedEvents(request.getSubscribedEvents());
        endpoint.setSecretKey(request.getSecretKey());
        endpoint.setHeadersJson(request.getHeadersJson());
        endpoint.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private WebhookEndpoint getEndpointEntity(UUID id) {
        return webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", id));
    }

    private WebhookEndpointDto mapEndpoint(WebhookEndpoint endpoint) {
        WebhookEndpointDto dto = new WebhookEndpointDto();
        dto.setId(endpoint.getId());
        dto.setName(endpoint.getName());
        dto.setUrl(endpoint.getUrl());
        dto.setSubscribedEvents(endpoint.getSubscribedEvents());
        dto.setSecretKey(endpoint.getSecretKey());
        dto.setHeadersJson(endpoint.getHeadersJson());
        dto.setActive(endpoint.getActive());
        dto.setCreatedAt(endpoint.getCreatedAt());
        return dto;
    }

    private WebhookDeliveryDto mapDelivery(WebhookDelivery delivery) {
        WebhookDeliveryDto dto = new WebhookDeliveryDto();
        dto.setId(delivery.getId());
        dto.setWebhookEndpointId(delivery.getWebhookEndpoint().getId());
        dto.setWebhookName(delivery.getWebhookEndpoint().getName());
        dto.setEventType(delivery.getEventType());
        dto.setStatus(delivery.getStatus());
        dto.setResponseStatus(delivery.getResponseStatus());
        dto.setResponseBody(delivery.getResponseBody());
        dto.setAttemptCount(delivery.getAttemptCount());
        dto.setDeliveredAt(delivery.getDeliveredAt());
        return dto;
    }
}