package com.inventory.system.config.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    public static final String DEFAULT_TENANT = "public";

    public static void setTenantId(String tenantId) {
        log.debug("Setting tenantId to {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get() != null ? CURRENT_TENANT.get() : DEFAULT_TENANT;
    }

    public static void clear() {
        log.debug("Clearing tenantId");
        CURRENT_TENANT.remove();
    }
}
