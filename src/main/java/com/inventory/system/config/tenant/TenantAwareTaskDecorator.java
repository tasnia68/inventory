package com.inventory.system.config.tenant;

import org.springframework.core.task.TaskDecorator;

/**
 * Propagates the current {@link TenantContext} from the submitting thread into the worker thread
 * used by any {@code @Async} executor that is configured with this decorator.
 *
 * <p>{@link TenantContext} is backed by a {@link ThreadLocal}, which is not inherited by new
 * threads automatically. Without this decorator, async tasks run without a tenant context,
 * causing Hibernate's {@code tenantFilter} to be inactive and potentially exposing cross-tenant
 * data or generating JWTs with the default "public" tenantId.
 */
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String tenantId = TenantContext.getTenantId();
        return () -> {
            TenantContext.setTenantId(tenantId);
            try {
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
