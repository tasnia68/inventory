package com.inventory.system.service;

import com.inventory.system.payload.StorefrontDomainContextDto;
import com.inventory.system.payload.StorefrontDomainDto;
import com.inventory.system.payload.StorefrontDomainRequest;

import java.util.Optional;
import java.util.UUID;

public interface StorefrontDomainService {
    StorefrontDomainContextDto getDomainContextForCurrentTenant();
    StorefrontDomainDto addDomain(StorefrontDomainRequest request);
    StorefrontDomainDto verifyDomain(UUID domainId);
    StorefrontDomainDto activateDomain(UUID domainId);
    void removeDomain(UUID domainId);
    Optional<String> resolveTenantIdForHost(String host);
    boolean isDomainAllowedForCaddy(String host);
    boolean isLocalDevelopmentHost(String host);
    Optional<String> getPrimaryStorefrontUrlForCurrentTenant();
}
