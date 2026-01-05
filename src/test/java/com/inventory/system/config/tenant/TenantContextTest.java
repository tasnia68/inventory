package com.inventory.system.config.tenant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TenantContextTest {

    @Test
    public void testTenantContext() {
        // Initial state
        Assertions.assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getTenantId());

        // Set tenant
        String tenantId = "test-tenant";
        TenantContext.setTenantId(tenantId);
        Assertions.assertEquals(tenantId, TenantContext.getTenantId());

        // Clear tenant
        TenantContext.clear();
        Assertions.assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getTenantId());
    }
}
