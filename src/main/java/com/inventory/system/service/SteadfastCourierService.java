package com.inventory.system.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryReviewStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShipmentQueueType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentQueueRefreshResultDto;
import com.inventory.system.payload.UpdateShipmentTrackingRequest;
import com.inventory.system.repository.CourierProfileRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.courier.CourierBookingResult;
import com.inventory.system.service.courier.CourierProviderException;
import com.inventory.system.service.courier.CourierReturnResult;
import com.inventory.system.service.courier.CourierStatusResult;
import com.inventory.system.service.courier.SteadfastCourierProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteadfastCourierService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentService shipmentService;
    private final CourierProfileRepository courierProfileRepository;
    private final SteadfastCourierProvider steadfastProvider;
    private final FinancialEventService financialEventService;

    @Transactional
    public Object bookShipment(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        CourierProfile profile = resolveProfile();
        CourierBookingResult booking;
        try {
            booking = steadfastProvider.bookShipment(shipment, profile);
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }

        UpdateShipmentTrackingRequest trackingUpdate = new UpdateShipmentTrackingRequest();
        trackingUpdate.setCourierProvider(booking.providerCode());
        trackingUpdate.setCourierReference(booking.courierReference());
        trackingUpdate.setTrackingNumber(booking.trackingNumber());
        trackingUpdate.setTrackingUrl(booking.trackingUrl());
        trackingUpdate.setCourierDispatchStatus(booking.dispatchStatus());
        trackingUpdate.setLastCourierEvent(booking.lastCourierEvent());
        trackingUpdate.setLastCourierSyncAt(booking.bookedAt());
        trackingUpdate.setTimelineSource("steadfast");
        return shipmentService.updateTracking(shipmentId, trackingUpdate);
    }

    @Transactional
    public Object syncStatus(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        CourierProfile profile = resolveProfile();
        CourierStatusResult status;
        try {
            status = steadfastProvider.syncStatus(shipment, profile);
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }

        UpdateShipmentTrackingRequest trackingUpdate = new UpdateShipmentTrackingRequest();
        trackingUpdate.setCourierDispatchStatus(status.dispatchStatus());
        trackingUpdate.setLastCourierEvent(status.lastCourierEvent());
        trackingUpdate.setLastCourierSyncAt(status.syncedAt());
        trackingUpdate.setTimelineSource("steadfast");
        trackingUpdate.setDeliveryReviewStatus(status.reviewStatus());
        if (status.reviewStatus() == DeliveryReviewStatus.PENDING) {
            trackingUpdate.setDeliveryReviewReason(status.reviewReason());
        } else if (shipment.getDeliveryReviewStatus() == DeliveryReviewStatus.PENDING) {
            trackingUpdate.setDeliveryReviewReason(null);
        }
        return shipmentService.updateTracking(shipmentId, trackingUpdate);
    }

    @Transactional
    public ShipmentQueueRefreshResultDto refreshQueue(ShipmentQueueType queue, int batchSize) {
        int normalizedBatchSize = Math.max(1, Math.min(batchSize, 200));
        List<ShipmentDto> queueShipments = new ArrayList<>(
                shipmentService.getShipmentsByQueue(queue, 0, normalizedBatchSize, "updatedAt", "desc").getContent()
        );

        ShipmentQueueRefreshResultDto result = new ShipmentQueueRefreshResultDto();
        result.setQueue(queue);
        result.setRequestedCount(queueShipments.size());
        result.setRefreshedAt(LocalDateTime.now());

        for (ShipmentDto shipment : queueShipments) {
            if (!isSteadfastShipment(shipment)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }

            try {
                if (shouldBookDuringQueueRefresh(queue, shipment)) {
                    bookShipment(shipment.getId());
                    result.setRefreshedCount(result.getRefreshedCount() + 1);
                    result.setBookedCount(result.getBookedCount() + 1);
                    continue;
                }

                if (shouldSyncDuringQueueRefresh(queue, shipment)) {
                    syncStatus(shipment.getId());
                    result.setRefreshedCount(result.getRefreshedCount() + 1);
                    result.setSyncedCount(result.getSyncedCount() + 1);
                    continue;
                }

                result.setSkippedCount(result.getSkippedCount() + 1);
            } catch (Exception ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                if (result.getFailures().size() < 10) {
                    result.getFailures().add(shipment.getShipmentNumber() + ": " + ex.getMessage());
                }
                log.warn("Steadfast queue refresh failed for queue {} shipment {}: {}", queue, shipment.getId(), ex.getMessage());
            }
        }

        return result;
    }

    @Transactional
    public CourierReturnResult requestReturn(UUID shipmentId, String reason) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));
        CourierProfile profile = resolveProfile();
        try {
            return steadfastProvider.requestReturn(shipment, profile, reason);
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Transactional
    public PaymentSyncResult syncPayments() {
        CourierProfile profile = resolveProfile();
        PaymentSyncResult result = new PaymentSyncResult();
        try {
            var payments = steadfastProvider.getPayments(profile);
            for (var payment : payments) {
                if (payment.getId() == null) continue;
                String reference = "STEADFAST:payment:" + payment.getId();
                BigDecimal gross = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
                BigDecimal fee = payment.resolvedFee();
                BigDecimal net = payment.getNetAmount() != null ? payment.getNetAmount() : gross.subtract(fee);
                try {
                    var posted = financialEventService.recordCourierSettlement(
                            reference, profile.getId(), gross, fee, net, null,
                            "Steadfast remittance " + payment.getId());
                    if (posted != null) result.posted++;
                    else result.skipped++;
                } catch (Exception ex) {
                    result.failed++;
                    if (result.failures.size() < 10) {
                        result.failures.add(reference + ": " + ex.getMessage());
                    }
                    log.warn("Steadfast payment posting failed for {}: {}", reference, ex.getMessage());
                }
                result.fetched++;
            }
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }
        return result;
    }

    @lombok.Data
    public static class PaymentSyncResult {
        private int fetched = 0;
        private int posted = 0;
        private int skipped = 0;
        private int failed = 0;
        private java.util.List<String> failures = new java.util.ArrayList<>();
    }

    public SteadfastBalanceResponse getBalance() {
        CourierProfile profile = resolveProfile();
        try {
            BigDecimal balance = steadfastProvider.getBalance(profile);
            SteadfastBalanceResponse response = new SteadfastBalanceResponse();
            response.setStatus(200);
            response.setCurrentBalance(balance);
            return response;
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private CourierProfile resolveProfile() {
        return courierProfileRepository
                .findFirstByProviderCodeIgnoreCaseAndIsActiveTrue(SteadfastCourierProvider.PROVIDER_CODE)
                .orElseGet(this::synthesizeLegacyProfile);
    }

    private CourierProfile synthesizeLegacyProfile() {
        // Legacy fallback: tenant settings steadfast.api_key / steadfast.secret_key are read directly
        // by SteadfastCourierProvider.buildHeaders() when credentialsJson is empty. An unpersisted
        // CourierProfile is enough to drive that fallback path for one release cycle, after which
        // V53 should seed a real STEADFAST CourierProfile per tenant.
        CourierProfile transientProfile = new CourierProfile();
        transientProfile.setProviderCode(SteadfastCourierProvider.PROVIDER_CODE);
        transientProfile.setDisplayName("Steadfast (legacy)");
        transientProfile.setActive(true);
        return transientProfile;
    }

    private boolean isSteadfastShipment(ShipmentDto shipment) {
        return shipment.getCourierProvider() != null
                && shipment.getCourierProvider().trim().equalsIgnoreCase(SteadfastCourierProvider.PROVIDER_CODE);
    }

    private boolean shouldBookDuringQueueRefresh(ShipmentQueueType queue, ShipmentDto shipment) {
        if (queue != ShipmentQueueType.READY_TO_HANDOFF) {
            return false;
        }
        String courierReference = shipment.getCourierReference();
        CourierDispatchStatus dispatchStatus = shipment.getCourierDispatchStatus();
        return (courierReference == null || courierReference.isBlank())
                && (dispatchStatus == null
                || dispatchStatus == CourierDispatchStatus.UNASSIGNED
                || dispatchStatus == CourierDispatchStatus.BOOKED
                || dispatchStatus == CourierDispatchStatus.PICKUP_PENDING);
    }

    private boolean shouldSyncDuringQueueRefresh(ShipmentQueueType queue, ShipmentDto shipment) {
        if (shipment.getCourierReference() == null || shipment.getCourierReference().isBlank()) {
            return false;
        }

        CourierDispatchStatus dispatchStatus = shipment.getCourierDispatchStatus();
        if (queue == ShipmentQueueType.READY_TO_HANDOFF) {
            return dispatchStatus == CourierDispatchStatus.BOOKED || dispatchStatus == CourierDispatchStatus.PICKUP_PENDING;
        }
        if (queue == ShipmentQueueType.IN_TRANSIT) {
            return dispatchStatus == CourierDispatchStatus.PICKED_UP
                    || dispatchStatus == CourierDispatchStatus.IN_TRANSIT
                    || dispatchStatus == CourierDispatchStatus.OUT_FOR_DELIVERY;
        }
        return true;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastBalanceResponse {
        private int status;
        @JsonProperty("current_balance")
        private BigDecimal currentBalance;
    }
}
