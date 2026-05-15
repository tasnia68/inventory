package com.inventory.system.config.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TenantAwareTaskDecoratorTest {

    private final TenantAwareTaskDecorator decorator = new TenantAwareTaskDecorator();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void propagatesTenantContextIntoDecoratedTask() {
        TenantContext.setTenantId("tenant-a");

        final String[] seenTenant = new String[1];
        Runnable decorated = decorator.decorate(() -> seenTenant[0] = TenantContext.getTenantId());

        TenantContext.clear();
        decorated.run();

        Assertions.assertEquals("tenant-a", seenTenant[0]);
        Assertions.assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    void leavesWorkerThreadWithoutSyntheticDefaultWhenNoTenantExists() {
        Runnable decorated = decorator.decorate(() ->
                Assertions.assertThrows(IllegalStateException.class, TenantContext::getTenantId));

        decorated.run();

        Assertions.assertNull(TenantContext.getCurrentTenantId());
    }
}