package com.inventory.system.service;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.ShipmentQueueType;
import com.inventory.system.common.entity.ShipmentStatus;
import com.inventory.system.common.entity.Tenant;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ShipmentQueueRefreshResultDto;
import com.inventory.system.payload.ShipmentQueueSummaryDto;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteadfastAutomationScheduler {

    private static final String PROVIDER_STEADFAST = "STEADFAST";
    private static final int AUTO_BOOK_BATCH_SIZE = 50;
    private static final int AUTO_SYNC_BATCH_SIZE = 100;
    private static final int AUTO_NEEDS_ACTION_BATCH_SIZE = 50;

    private final TenantRepository tenantRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final ShipmentRepository shipmentRepository;
    private final SteadfastCourierService steadfastCourierService;
    private final ShipmentService shipmentService;

    @Scheduled(fixedDelayString = "${app.fulfillment.steadfast.auto-book-fixed-delay-ms:60000}")
    public void autoBookReadyShipments() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }

            String tenantId = tenant.getId().toString();
            if (!isTenantBooleanEnabled(tenantId, "steadfast.auto_book_enabled", false)) {
                continue;
            }

            TenantContext.setTenantId(tenantId);
            try {
                ShipmentQueueRefreshResultDto result = steadfastCourierService.refreshQueue(
                        ShipmentQueueType.READY_TO_HANDOFF,
                        AUTO_BOOK_BATCH_SIZE
                );
                logQueueRefresh("auto-book", tenantId, result);
            } catch (Exception ex) {
                log.error("Auto-book sweep failed for tenant {}: {}", tenantId, ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.fulfillment.steadfast.auto-payment-sync-fixed-delay-ms:1800000}")
    public void autoSyncPayments() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }
            String tenantId = tenant.getId().toString();
            if (!isTenantBooleanEnabled(tenantId, "steadfast.auto_payment_sync_enabled", false)) {
                continue;
            }
            TenantContext.setTenantId(tenantId);
            try {
                SteadfastCourierService.PaymentSyncResult result = steadfastCourierService.syncPayments();
                log.info(
                        "Steadfast payment sync for tenant {}: fetched={}, posted={}, skipped={}, failed={}",
                        tenantId, result.getFetched(), result.getPosted(), result.getSkipped(), result.getFailed()
                );
                if (result.getFailed() > 0 && !result.getFailures().isEmpty()) {
                    log.warn("Steadfast payment sync failures for tenant {}: {}", tenantId, result.getFailures());
                }
            } catch (Exception ex) {
                log.error("Auto payment sync failed for tenant {}: {}", tenantId, ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.fulfillment.steadfast.auto-sync-fixed-delay-ms:300000}")
    public void autoSyncShipmentStatuses() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }

            String tenantId = tenant.getId().toString();
            if (!isTenantBooleanEnabled(tenantId, "steadfast.auto_status_sync_enabled", false)) {
                continue;
            }

            int syncIntervalMinutes = getTenantIntSetting(
                    tenantId,
                    "steadfast.auto_status_sync_interval_minutes",
                    15,
                    1,
                    720
            );

            LocalDateTime syncBefore = LocalDateTime.now().minusMinutes(syncIntervalMinutes);

            TenantContext.setTenantId(tenantId);
            try {
                ShipmentQueueRefreshResultDto readyToHandoff = steadfastCourierService.refreshQueue(
                        ShipmentQueueType.READY_TO_HANDOFF,
                        Math.min(AUTO_BOOK_BATCH_SIZE, AUTO_SYNC_BATCH_SIZE)
                );
                ShipmentQueueRefreshResultDto inTransit = steadfastCourierService.refreshQueue(
                        ShipmentQueueType.IN_TRANSIT,
                        AUTO_SYNC_BATCH_SIZE
                );
                ShipmentQueueRefreshResultDto needsAction = steadfastCourierService.refreshQueue(
                        ShipmentQueueType.NEEDS_ACTION,
                        AUTO_NEEDS_ACTION_BATCH_SIZE
                );

                logQueueRefresh("auto-sync-ready", tenantId, readyToHandoff);
                logQueueRefresh("auto-sync-transit", tenantId, inTransit);
                logQueueRefresh("auto-sync-needs-action", tenantId, needsAction);

                ShipmentQueueSummaryDto summary = shipmentService.getShipmentQueueSummary();
                log.info(
                        "Steadfast queue summary for tenant {} after sync: readyToHandoff={}, inTransit={}, needsAction={}, reviewPending={}, reviewDisputed={}",
                        tenantId,
                        summary.getReadyToHandoffCount(),
                        summary.getInTransitCount(),
                        summary.getNeedsActionCount(),
                        summary.getDeliveryReviewPendingCount(),
                        summary.getDeliveryReviewDisputedCount()
                );
            } catch (Exception ex) {
                log.error("Auto-sync sweep failed for tenant {}: {}", tenantId, ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private boolean isTenantBooleanEnabled(String tenantId, String settingKey, boolean defaultValue) {
        Optional<String> value = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .map(setting -> setting.getSettingValue());

        if (value.isEmpty() || value.get() == null || value.get().isBlank()) {
            return defaultValue;
        }

        String normalized = value.get().trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true")
                || normalized.equals("1")
                || normalized.equals("yes")
                || normalized.equals("on");
    }

    private int getTenantIntSetting(String tenantId, String settingKey, int defaultValue, int min, int max) {
        Optional<String> value = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .map(setting -> setting.getSettingValue());

        if (value.isEmpty() || value.get() == null || value.get().isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.get().trim());
            if (parsed < min) {
                return min;
            }
            if (parsed > max) {
                return max;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            log.warn("Invalid integer setting for tenant {} key {}: {}", tenantId, settingKey, value.get());
            return defaultValue;
        }
    }

    private void logQueueRefresh(String operation, String tenantId, ShipmentQueueRefreshResultDto result) {
        log.info(
                "Steadfast {} queue refresh for tenant {} queue {}: requested={}, refreshed={}, booked={}, synced={}, skipped={}, failed={}",
                operation,
                tenantId,
                result.getQueue(),
                result.getRequestedCount(),
                result.getRefreshedCount(),
                result.getBookedCount(),
                result.getSyncedCount(),
                result.getSkippedCount(),
                result.getFailedCount()
        );
    }
}