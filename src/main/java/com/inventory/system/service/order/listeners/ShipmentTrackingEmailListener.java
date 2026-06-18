package com.inventory.system.service.order.listeners;

import com.inventory.system.service.order.events.ShipmentTrackingUpdatedEvent;
import com.inventory.system.service.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Turns a tracking-updated domain event into a durable outbox command, so the customer
 * tracking email is delivered with retry/back-off rather than best-effort inline. The
 * relay performs the send and the once-only guard ({@code trackingNotifiedAt}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentTrackingEmailListener {

    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTrackingUpdated(ShipmentTrackingUpdatedEvent event) {
        if (event.shipmentId() == null) {
            return;
        }
        outboxService.enqueue(OutboxService.TYPE_TRACKING_EMAIL,
                Map.of("shipmentId", event.shipmentId().toString()));
    }
}
