package com.inventory.system.service;

import com.inventory.system.payload.SuperAdminTenantRequest;
import com.inventory.system.payload.SuperAdminTenantUpdateRequest;
import com.inventory.system.payload.TenantResponse;

import java.util.List;
import java.util.UUID;

public interface SuperAdminTenantService {
    List<TenantResponse> getAllTenants();
    TenantResponse createTenant(SuperAdminTenantRequest request);
    TenantResponse updateTenant(UUID tenantId, SuperAdminTenantUpdateRequest request);
    TenantResponse activateTenant(UUID tenantId);
    TenantResponse deactivateTenant(UUID tenantId);
    void deleteTenant(UUID tenantId);
}
