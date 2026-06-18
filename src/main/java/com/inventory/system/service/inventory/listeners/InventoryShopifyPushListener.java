package com.inventory.system.service.inventory.listeners;

import com.inventory.system.service.inventory.events.InventoryChangedEvent;
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
 * Turns a local inventory change into a durable outbox command to push the new on-hand
 * to Shopify, so a sale on any channel is mirrored to the Shopify store with retry.
 *
 * <p>Echo-suppression: changes originating from a Shopify pull/sync carry a referenceId
 * starting with "shopify" and are NOT enqueued, preventing webhook/sync ping-pong.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryShopifyPushListener {

    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInventoryChanged(InventoryChangedEvent event) {
        String ref = event.referenceId();
        if (ref != null && ref.toLowerCase().startsWith("shopify")) {
            return; // echo from a Shopify-originated change — do not push back
        }
        if (event.productVariantId() == null || event.warehouseId() == null) {
            return;
        }
        outboxService.enqueue(OutboxService.TYPE_SHOPIFY_INVENTORY, Map.of(
                "variantId", event.productVariantId().toString(),
                "warehouseId", event.warehouseId().toString()));
    }
}
