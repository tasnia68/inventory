package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontDomainRepository extends JpaRepository<StorefrontDomain, UUID> {
    Optional<StorefrontDomain> findFirstByHostnameIgnoreCase(String hostname);
    Optional<StorefrontDomain> findFirstByHostnameIgnoreCaseAndActiveTrue(String hostname);
    List<StorefrontDomain> findByTenantIdOrderByPrimaryDescHostnameAsc(UUID tenantId);
    boolean existsByHostnameIgnoreCase(String hostname);
}
