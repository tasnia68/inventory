package com.inventory.system.service;

import com.inventory.system.payload.TenantRequest;
import com.inventory.system.payload.TenantResponse;

public interface TenantService {
    TenantResponse registerTenant(TenantRequest request);
}
