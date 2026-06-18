package com.inventory.system.service.inventory.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised whenever a variant's on-hand stock changes at a warehouse — the single
 * chokepoint being {@code StockService.adjustStock}. {@code referenceId} carries the
 * movement's origin so downstream propagation can suppress echoes (e.g. a change that
 * came from a Shopify pull must not be pushed back to Shopify).
 */
public record InventoryChangedEvent(UUID productVariantId, UUID warehouseId, String referenceId, Instant occurredAt) {
}
