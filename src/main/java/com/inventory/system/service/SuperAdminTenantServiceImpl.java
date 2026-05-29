package com.inventory.system.service;

import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.SuperAdminTenantRequest;
import com.inventory.system.payload.SuperAdminTenantUpdateRequest;
import com.inventory.system.payload.TenantRequest;
import com.inventory.system.payload.TenantResponse;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminTenantServiceImpl implements SuperAdminTenantService {

    private static final String STOREFRONT_MODULE_ENABLED_KEY = "tenant.modules.storefront.enabled";
    private static final String STOREFRONT_CATEGORY = "STOREFRONT";

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final TenantSettingService tenantSettingService;
    private final TenantSettingRepository tenantSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TenantResponse createTenant(SuperAdminTenantRequest request) {
        TenantRequest tenantRequest = new TenantRequest();
        tenantRequest.setName(request.getName());
        tenantRequest.setSubdomain(request.getSubdomain());
        tenantRequest.setStorefrontEnabled(request.getStorefrontEnabled());
        tenantRequest.setPlan(request.getPlan());
        tenantRequest.setAdminEmail(request.getAdminEmail());
        tenantRequest.setAdminPassword(request.getAdminPassword());
        return tenantService.registerTenant(tenantRequest);
    }

    @Override
    @Transactional
    public TenantResponse updateTenant(UUID tenantId, SuperAdminTenantUpdateRequest request) {
        Tenant tenant = getTenant(tenantId);

        tenantRepository.findBySubdomainIgnoreCase(request.getSubdomain())
                .filter(existing -> !existing.getId().equals(tenant.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Subdomain already exists");
                });

        tenant.setName(request.getName().trim());
        tenant.setSubdomain(request.getSubdomain().trim().toLowerCase());
        tenant.setSubscriptionPlan(request.getPlan());
        tenant.setStatus(request.getStatus());
        tenantSettingService.updateSettingForTenant(
            tenant.getId().toString(),
            STOREFRONT_MODULE_ENABLED_KEY,
            Boolean.toString(Boolean.TRUE.equals(request.getStorefrontEnabled())),
            "BOOLEAN",
            STOREFRONT_CATEGORY);

        return mapToResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public TenantResponse activateTenant(UUID tenantId) {
        Tenant tenant = getTenant(tenantId);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        return mapToResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public TenantResponse deactivateTenant(UUID tenantId) {
        Tenant tenant = getTenant(tenantId);
        tenant.setStatus(Tenant.TenantStatus.INACTIVE);
        return mapToResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = getTenant(tenantId);
        tenantRepository.delete(tenant);
    }

    private Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSubdomain(tenant.getSubdomain());
        response.setStorefrontEnabled(isStorefrontEnabled(tenant.getId().toString()));
        response.setStatus(tenant.getStatus());
        response.setSubscriptionPlan(tenant.getSubscriptionPlan());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }

    private boolean isStorefrontEnabled(String tenantId) {
        // Bypass the Hibernate tenantFilter: the super-admin lists every tenant
        // while running in the platform tenant's context, so a filtered query
        // would never return another tenant's row (every tenant would render
        // as Disabled regardless of its actual setting).
        return tenantSettingRepository
                .findValueByTenantIdAndSettingKey(tenantId, STOREFRONT_MODULE_ENABLED_KEY)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }
}
