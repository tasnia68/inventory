package com.inventory.system.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.OutboxEvent;
import com.inventory.system.common.entity.OutboxStatus;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Writes durable side-effect commands to the transactional outbox. Joins the caller's
 * transaction (REQUIRED) so the command is committed atomically with the work that
 * produced it; {@link OutboxRelay} then delivers it with retry/back-off.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    public static final String TYPE_SHOPIFY_INVENTORY = "SHOPIFY_INVENTORY_PUSH";
    public static final String TYPE_SHOPIFY_FULFILLMENT = "SHOPIFY_FULFILLMENT_PUSH";
    public static final String TYPE_TRACKING_EMAIL = "TRACKING_EMAIL";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueue(String type, Map<String, Object> payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setTenantId(TenantContext.getTenantId());
            event.setEventType(type);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxStatus.PENDING);
            event.setNextAttemptAt(LocalDateTime.now());
            repository.save(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enqueue outbox event " + type, e);
        }
    }
}
