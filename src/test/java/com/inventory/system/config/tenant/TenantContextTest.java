package com.inventory.system.config.tenant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TenantContextTest {

    @BeforeEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    public void returnsNullWhenTenantIsNotSet() {
        Assertions.assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    public void rejectsBlankTenantAssignments() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TenantContext.setTenantId(" "));
    }

    @Test
    public void requiresTenantWhenAccessingTenantScopedContext() {
        Assertions.assertThrows(IllegalStateException.class, TenantContext::getTenantId);
        Assertions.assertThrows(IllegalStateException.class, TenantContext::requireTenantId);
    }

    @Test
    public void testTenantContextLifecycle() {
        Assertions.assertNull(TenantContext.getCurrentTenantId());

        String tenantId = "test-tenant";
        TenantContext.setTenantId(tenantId);
        Assertions.assertEquals(tenantId, TenantContext.getTenantId());
        Assertions.assertEquals(tenantId, TenantContext.getCurrentTenantId());

        TenantContext.clear();
        Assertions.assertNull(TenantContext.getCurrentTenantId());
        Assertions.assertThrows(IllegalStateException.class, TenantContext::getTenantId);
    }

    @Test
    public void scopedExecutionRestoresPreviousTenant() {
        TenantContext.setTenantId("outer-tenant");

        String innerTenant = TenantContext.callWithTenant("inner-tenant", TenantContext::getTenantId);

        Assertions.assertEquals("inner-tenant", innerTenant);
        Assertions.assertEquals("outer-tenant", TenantContext.getTenantId());
    }

    @Test
    public void concurrentThreadsKeepIndependentTenantContexts() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<String> tenantA = buildConcurrentTenantTask("tenant-a", ready, start);
            Callable<String> tenantB = buildConcurrentTenantTask("tenant-b", ready, start);

            Future<String> futureA = executor.submit(tenantA);
            Future<String> futureB = executor.submit(tenantB);

            Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<String> results = new ArrayList<>();
            results.add(futureA.get(5, TimeUnit.SECONDS));
            results.add(futureB.get(5, TimeUnit.SECONDS));

            Assertions.assertTrue(results.contains("tenant-a"));
            Assertions.assertTrue(results.contains("tenant-b"));
            Assertions.assertNull(TenantContext.getCurrentTenantId());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> buildConcurrentTenantTask(String tenantId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            Assertions.assertTrue(start.await(5, TimeUnit.SECONDS));
            return TenantContext.callWithTenant(tenantId, TenantContext::getTenantId);
        };
    }
}
