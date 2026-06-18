package com.inventory.system.service;

import com.inventory.system.config.scaling.RabbitConfig;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ShopifySyncJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes async Shopify sync jobs and drives the run to completion server-side, so a
 * sync of thousands of products no longer depends on a browser staying open and can be
 * spread across however many backend workers are running.
 *
 * <p>Tenant context is set from the job before any DB work, so the {@code TenantFilterAspect}
 * enables the Hibernate tenant filter on the worker thread exactly as it does per HTTP request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scaling.enabled", havingValue = "true")
public class ShopifySyncWorker {

    private final ShopifyIntegrationService shopifyIntegrationService;

    @RabbitListener(queues = RabbitConfig.SYNC_QUEUE)
    public void onSyncJob(ShopifySyncJob job) {
        if (job == null || job.getTenantId() == null || job.getRunId() == null) {
            log.warn("Discarding malformed Shopify sync job: {}", job);
            return;
        }
        TenantContext.runWithTenant(job.getTenantId(), () -> {
            log.info("Processing Shopify {} sync job for run {}", job.getSyncType(), job.getRunId());
            shopifyIntegrationService.driveRunToCompletion(UUID.fromString(job.getRunId()));
        });
    }
}
