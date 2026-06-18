package com.inventory.system.service.order.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised whenever a shipment gains (or changes) courier tracking — from a manual
 * tracking update, courier auto-booking, or an inbound courier status webhook.
 * Consumed centrally to notify the customer with the tracking link.
 */
public record ShipmentTrackingUpdatedEvent(UUID shipmentId, UUID salesOrderId, Instant occurredAt) {
}
