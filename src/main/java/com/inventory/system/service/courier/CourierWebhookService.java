package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.order.events.ShipmentTrackingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Applies inbound courier status callbacks (e.g. Steadfast webhooks) to the matching
 * shipment, so delivery progress flows in automatically instead of being polled or
 * pasted by hand. Tenant scoping is enforced by the surrounding TenantContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourierWebhookService {

    private final ShipmentRepository shipmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Returns true if a shipment matched and was updated. */
    @Transactional
    public boolean applySteadfastStatus(String consignmentId, String trackingCode, String deliveryStatus) {
        Shipment shipment = null;
        if (consignmentId != null && !consignmentId.isBlank()) {
            shipment = shipmentRepository
                    .findFirstByCourierProviderIgnoreCaseAndCourierReference(
                            SteadfastCourierProvider.PROVIDER_CODE, consignmentId.trim())
                    .orElse(null);
        }
        if (shipment == null && trackingCode != null && !trackingCode.isBlank()) {
            shipment = shipmentRepository.findFirstByTrackingNumber(trackingCode.trim()).orElse(null);
        }
        if (shipment == null) {
            log.info("Steadfast webhook: no matching shipment (consignment={}, tracking={})", consignmentId, trackingCode);
            return false;
        }

        CourierDispatchStatus mapped = SteadfastCourierProvider.mapDispatchStatus(deliveryStatus);
        shipment.setCourierDispatchStatus(mapped);
        shipment.setLastCourierEvent("steadfast:" + deliveryStatus);
        shipment.setLastCourierSyncAt(LocalDateTime.now());
        if (mapped == CourierDispatchStatus.DELIVERED && shipment.getDeliveredDate() == null) {
            shipment.setDeliveredDate(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        // A booked consignment carries a tracking link; let the central handler email it.
        eventPublisher.publishEvent(new ShipmentTrackingUpdatedEvent(
                shipment.getId(),
                shipment.getSalesOrder() != null ? shipment.getSalesOrder().getId() : null,
                Instant.now()));
        return true;
    }
}
