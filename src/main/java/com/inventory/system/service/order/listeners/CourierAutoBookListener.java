package com.inventory.system.service.order.listeners;

import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.repository.CourierProfileRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.TenantSettingService;
import com.inventory.system.service.courier.CourierBookingResult;
import com.inventory.system.service.courier.CourierProvider;
import com.inventory.system.service.courier.CourierProviderRegistry;
import com.inventory.system.service.courier.ManualCourierProvider;
import com.inventory.system.service.order.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourierAutoBookListener {

    private static final String AUTO_BOOK_ON_CONFIRMED_KEY = "courier.autoBookOnConfirmed";

    private final SalesOrderRepository salesOrderRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final ShipmentRepository shipmentRepository;
    private final CourierProviderRegistry providerRegistry;
    private final TenantSettingService tenantSettingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.toStatus() != SalesOrderStatus.CONFIRMED) return;
        if (!autoBookEnabled()) return;

        SalesOrder salesOrder = salesOrderRepository.findById(event.salesOrderId()).orElse(null);
        if (salesOrder == null || salesOrder.getCourierProfileId() == null) {
            return;
        }

        CourierProfile profile = courierProfileRepository.findById(salesOrder.getCourierProfileId()).orElse(null);
        if (profile == null || ManualCourierProvider.PROVIDER_CODE.equalsIgnoreCase(profile.getProviderCode())) {
            return;
        }

        List<Shipment> shipments = shipmentRepository.findBySalesOrderId(salesOrder.getId());
        Shipment shipment = shipments.stream()
                .filter(s -> s.getCourierReference() == null || s.getCourierReference().isBlank())
                .findFirst()
                .orElse(null);
        if (shipment == null) {
            log.debug("Auto-book skipped for {}: no shipment without a courier reference", salesOrder.getSoNumber());
            return;
        }

        try {
            CourierProvider provider = providerRegistry.resolve(profile.getProviderCode());
            CourierBookingResult booking = provider.bookShipment(shipment, profile);
            shipment.setCourierProvider(booking.providerCode());
            shipment.setCourierReference(booking.courierReference());
            shipment.setTrackingNumber(booking.trackingNumber());
            shipment.setTrackingUrl(booking.trackingUrl());
            shipment.setCourierDispatchStatus(booking.dispatchStatus());
            shipment.setLastCourierEvent(booking.lastCourierEvent());
            shipment.setLastCourierSyncAt(booking.bookedAt());
            shipmentRepository.save(shipment);
        } catch (Exception e) {
            log.error("Auto-book failed for order {}: {}", salesOrder.getSoNumber(), e.getMessage(), e);
        }
    }

    private boolean autoBookEnabled() {
        return tenantSettingService.findSetting(AUTO_BOOK_ON_CONFIRMED_KEY)
                .map(s -> s.getValue())
                .map(v -> "true".equalsIgnoreCase(v.trim()))
                .orElse(false);
    }
}
