package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductTemplateRepository extends JpaRepository<ProductTemplate, UUID> {
	Optional<ProductTemplate> findFirstByTenantIdAndStorefrontSlug(String tenantId, String storefrontSlug);

	default Optional<ProductTemplate> findFirstByTenantIdAndStorefrontSlug(String storefrontSlug) {
		return findFirstByTenantIdAndStorefrontSlug(com.inventory.system.config.tenant.TenantContext.getTenantId(), storefrontSlug);
	}
}
