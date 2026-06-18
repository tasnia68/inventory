package com.inventory.system.service.order.listeners;

import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.order.events.OrderStatusChangedEvent;
import com.inventory.system.service.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * When a Shopify-origin order ships locally, enqueue a durable command to mirror the
 * fulfillment (with the courier tracking link) back to Shopify and let Shopify notify
 * the customer. The relay delivers it with retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopifyFulfillmentPushListener {

    private final SalesOrderRepository salesOrderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderShipped(OrderStatusChangedEvent event) {
        if (event.toStatus() != SalesOrderStatus.SHIPPED) {
            return;
        }
        SalesOrder order = salesOrderRepository.findById(event.salesOrderId()).orElse(null);
        if (order == null
                || order.getExternalSource() != ExternalOrderSource.SHOPIFY
                || order.getExternalOrderId() == null) {
            return; // only mirror fulfillment back for Shopify-origin orders
        }

        List<Shipment> shipments = shipmentRepository.findBySalesOrderId(order.getId());
        Shipment shipment = shipments.stream()
                .filter(s -> s.getTrackingNumber() != null || s.getTrackingUrl() != null)
                .findFirst()
                .orElse(shipments.stream().findFirst().orElse(null));

        Map<String, Object> payload = new HashMap<>();
        payload.put("externalOrderId", order.getExternalOrderId());
        if (shipment != null) {
            if (shipment.getTrackingNumber() != null) payload.put("trackingNumber", shipment.getTrackingNumber());
            if (shipment.getTrackingUrl() != null) payload.put("trackingUrl", shipment.getTrackingUrl());
            String carrier = shipment.getCourierProvider() != null && !shipment.getCourierProvider().isBlank()
                    ? shipment.getCourierProvider() : shipment.getCarrier();
            if (carrier != null) payload.put("carrier", carrier);
        }
        outboxService.enqueue(OutboxService.TYPE_SHOPIFY_FULFILLMENT, payload);
    }
}
