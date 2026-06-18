package com.inventory.system.service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.OutboxEvent;
import com.inventory.system.common.entity.OutboxStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.config.scaling.DistributedLockService;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.OutboxEventRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.EmailService;
import com.inventory.system.service.ShopifyIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Polls the outbox and delivers pending commands with retry/back-off, sending exhausted
 * ones to a FAILED dead-letter state. Runs at most one instance at a time via a Redis
 * lock (no-op single-instance when scaling is off), and re-establishes the row's tenant
 * context before the transactional {@code process} so tenant-scoped reads stay correct.
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 6;
    private static final String LOCK_KEY = "outbox:relay";

    private final OutboxEventRepository repository;
    private final DistributedLockService lockService;
    private final ObjectMapper objectMapper;
    private final ShopifyIntegrationService shopifyIntegrationService;
    private final EmailService emailService;
    private final ShipmentRepository shipmentRepository;
    private final ObjectProvider<OutboxRelay> self;
    private final String owner = UUID.randomUUID().toString();

    public OutboxRelay(OutboxEventRepository repository,
                       DistributedLockService lockService,
                       ObjectMapper objectMapper,
                       ShopifyIntegrationService shopifyIntegrationService,
                       EmailService emailService,
                       ShipmentRepository shipmentRepository,
                       ObjectProvider<OutboxRelay> self) {
        this.repository = repository;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
        this.shopifyIntegrationService = shopifyIntegrationService;
        this.emailService = emailService;
        this.shipmentRepository = shipmentRepository;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:15000}")
    public void poll() {
        if (!lockService.tryAcquire(LOCK_KEY, owner, Duration.ofSeconds(30))) {
            return; // another instance holds the relay lock
        }
        try {
            List<OutboxEvent> batch = repository.findDispatchable(
                    OutboxStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
            for (OutboxEvent event : batch) {
                String tenantId = event.getTenantId();
                UUID id = event.getId();
                try {
                    // Tenant context must be set BEFORE the proxied @Transactional call so the
                    // tenant filter is enabled for the dispatch's reads.
                    TenantContext.runWithTenant(tenantId, () -> self.getObject().process(id));
                } catch (Exception e) {
                    log.error("Outbox relay failed for event {}: {}", id, e.getMessage());
                }
            }
        } finally {
            lockService.release(LOCK_KEY, owner);
        }
    }

    @Transactional
    public void process(UUID eventId) {
        OutboxEvent event = repository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }
        try {
            dispatch(event);
            event.setStatus(OutboxStatus.SENT);
        } catch (Exception ex) {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(truncate(ex.getMessage()));
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} ({}) dead-lettered after {} attempts: {}",
                        event.getId(), event.getEventType(), event.getAttempts(), ex.getMessage());
            } else {
                long backoffMinutes = (long) Math.min(60, Math.pow(2, event.getAttempts()));
                event.setNextAttemptAt(LocalDateTime.now().plusMinutes(backoffMinutes));
            }
        }
        event.setUpdatedAt(LocalDateTime.now());
        repository.save(event);
    }

    private void dispatch(OutboxEvent event) throws Exception {
        JsonNode p = objectMapper.readTree(event.getPayload() == null ? "{}" : event.getPayload());
        switch (event.getEventType()) {
            case OutboxService.TYPE_SHOPIFY_INVENTORY ->
                    shopifyIntegrationService.pushInventoryForVariant(uuid(p, "variantId"), uuid(p, "warehouseId"));
            case OutboxService.TYPE_SHOPIFY_FULFILLMENT ->
                    shopifyIntegrationService.pushFulfillment(
                            text(p, "externalOrderId"), text(p, "trackingNumber"),
                            text(p, "trackingUrl"), text(p, "carrier"));
            case OutboxService.TYPE_TRACKING_EMAIL -> dispatchTrackingEmail(uuid(p, "shipmentId"));
            default -> log.warn("Unknown outbox event type: {}", event.getEventType());
        }
    }

    private void dispatchTrackingEmail(UUID shipmentId) {
        if (shipmentId == null) {
            return;
        }
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getTrackingUrl() == null || shipment.getTrackingUrl().isBlank()) {
            return;
        }
        if (shipment.getTrackingNotifiedAt() != null) {
            return; // already emailed
        }
        SalesOrder order = shipment.getSalesOrder();
        Customer customer = order != null ? order.getCustomer() : null;
        String email = customer != null ? customer.getEmail() : null;
        if (email == null || email.isBlank()) {
            return;
        }
        String courier = shipment.getCourierProvider() != null && !shipment.getCourierProvider().isBlank()
                ? shipment.getCourierProvider() : shipment.getCarrier();
        emailService.sendTrackingEmail(email, customer.getName(), order.getSoNumber(), shipment.getTrackingUrl(), courier);
        shipment.setTrackingNotifiedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);
    }

    private static UUID uuid(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : UUID.fromString(node.asText());
    }

    private static String text(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
