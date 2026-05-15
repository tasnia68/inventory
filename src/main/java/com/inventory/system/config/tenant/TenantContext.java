package com.inventory.system.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.Callable;
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

    public static Runnable wrap(Runnable action) {
        String capturedTenantId = getCurrentTenantId();
        return () -> runCaptured(capturedTenantId, action);
    }

    public static <T> Supplier<T> wrapSupplier(Supplier<T> action) {
        String capturedTenantId = getCurrentTenantId();
        return () -> callCaptured(capturedTenantId, action);
    }

    public static <T> Callable<T> wrapCallable(Callable<T> action) {
        String capturedTenantId = getCurrentTenantId();
        return () -> callCaptured(capturedTenantId, action);
    }

    public static void clear() {
        log.debug("Clearing tenantId");
        CURRENT_TENANT.remove();
    }

    private static void runCaptured(String tenantId, Runnable action) {
        String previousTenantId = getCurrentTenantId();
        applyCaptured(tenantId);
        try {
            action.run();
        } finally {
            restore(previousTenantId);
        }
    }

    private static <T> T callCaptured(String tenantId, Supplier<T> action) {
        String previousTenantId = getCurrentTenantId();
        applyCaptured(tenantId);
        try {
            return action.get();
        } finally {
            restore(previousTenantId);
        }
    }

    private static <T> T callCaptured(String tenantId, Callable<T> action) throws Exception {
        String previousTenantId = getCurrentTenantId();
        applyCaptured(tenantId);
        try {
            return action.call();
        } finally {
            restore(previousTenantId);
        }
    }

    private static void applyCaptured(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            CURRENT_TENANT.set(tenantId);
            return;
        }
        clear();
    }

    private static void restore(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            CURRENT_TENANT.set(tenantId);
            return;
        }
        clear();
    }
}
