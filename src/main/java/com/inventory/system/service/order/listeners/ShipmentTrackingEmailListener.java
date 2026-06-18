package com.inventory.system.service.order.listeners;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.EmailService;
import com.inventory.system.service.order.events.ShipmentTrackingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Central handler that emails the customer their courier tracking link once a
 * shipment has one. Fires after the publishing transaction commits, is idempotent
 * (sends at most once per shipment via {@code trackingNotifiedAt}), and never
 * fails the originating operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentTrackingEmailListener {

    private final ShipmentRepository shipmentRepository;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTrackingUpdated(ShipmentTrackingUpdatedEvent event) {
        Shipment shipment = shipmentRepository.findById(event.shipmentId()).orElse(null);
        if (shipment == null) {
            return;
        }
        if (shipment.getTrackingUrl() == null || shipment.getTrackingUrl().isBlank()) {
            return;
        }
        if (shipment.getTrackingNotifiedAt() != null) {
            return; // already emailed the customer for this shipment
        }

        SalesOrder order = shipment.getSalesOrder();
        Customer customer = order != null ? order.getCustomer() : null;
        String email = customer != null ? customer.getEmail() : null;
        if (email == null || email.isBlank()) {
            log.debug("Tracking email skipped for shipment {}: no customer email", shipment.getId());
            return;
        }

        String courier = shipment.getCourierProvider() != null && !shipment.getCourierProvider().isBlank()
                ? shipment.getCourierProvider()
                : shipment.getCarrier();
        try {
            emailService.sendTrackingEmail(
                    email,
                    customer.getName(),
                    order.getSoNumber(),
                    shipment.getTrackingUrl(),
                    courier);
            shipment.setTrackingNotifiedAt(LocalDateTime.now());
            shipmentRepository.save(shipment);
            log.info("Tracking email sent for shipment {} (order {})", shipment.getShipmentNumber(), order.getSoNumber());
        } catch (Exception e) {
            log.error("Failed to send tracking email for shipment {}: {}", shipment.getId(), e.getMessage());
        }
    }
}
