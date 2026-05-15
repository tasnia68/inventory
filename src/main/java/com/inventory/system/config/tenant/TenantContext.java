package com.inventory.system.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        log.debug("Setting tenantId to {}", tenantId);
        CURRENT_TENANT.set(tenantId.trim());
    }

    public static String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static String getTenantId() {
        return requireTenantId();
    }

    public static String requireTenantId() {
        String tenantId = getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("Tenant context is required but was not set for the current execution");
        }
        return tenantId;
    }

    public static void runWithTenant(String tenantId, Runnable action) {
        callWithTenant(tenantId, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callWithTenant(String tenantId, Supplier<T> action) {
        String previousTenantId = getCurrentTenantId();
        setTenantId(tenantId);
        try {
            return action.get();
        } finally {
            restore(previousTenantId);
        }
    }

    public static void clear() {
        log.debug("Clearing tenantId");
        CURRENT_TENANT.remove();
    }

    private static void restore(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            CURRENT_TENANT.set(tenantId);
            return;
        }
        clear();
    }
}
