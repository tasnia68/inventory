package com.inventory.system.config.tenant;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;

/**
 * Tenant-aware entry points for async APIs that do not propagate ThreadLocal state on their own.
 */
public final class TenantAsync {

    private static final Executor TENANT_AWARE_COMMON_POOL = tenantAware(ForkJoinPool.commonPool());

    private TenantAsync() {
    }

    public static Executor tenantAware(Executor delegate) {
        Objects.requireNonNull(delegate, "delegate executor must not be null");
        return command -> delegate.execute(TenantContext.wrap(command));
    }

    public static Executor commonPoolExecutor() {
        return TENANT_AWARE_COMMON_POOL;
    }

    public static CompletableFuture<Void> runAsync(Runnable action) {
        return CompletableFuture.runAsync(TenantContext.wrap(action), TENANT_AWARE_COMMON_POOL);
    }

    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        return CompletableFuture.runAsync(TenantContext.wrap(action), executor);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(TenantContext.wrapSupplier(supplier), TENANT_AWARE_COMMON_POOL);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(TenantContext.wrapSupplier(supplier), executor);
    }

    public static ForkJoinTask<?> submit(Runnable action) {
        return ForkJoinPool.commonPool().submit(TenantContext.wrap(action));
    }

    public static <T> ForkJoinTask<T> submit(Supplier<T> supplier) {
        return ForkJoinPool.commonPool().submit(TenantContext.wrapSupplier(supplier)::get);
    }

    public static ForkJoinTask<?> submit(ForkJoinPool pool, Runnable action) {
        Objects.requireNonNull(pool, "pool must not be null");
        return pool.submit(TenantContext.wrap(action));
    }

    public static <T> ForkJoinTask<T> submit(ForkJoinPool pool, Supplier<T> supplier) {
        Objects.requireNonNull(pool, "pool must not be null");
        return pool.submit(TenantContext.wrapSupplier(supplier)::get);
    }
}