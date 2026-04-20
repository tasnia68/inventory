package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontPageRepository extends JpaRepository<StorefrontPage, UUID> {
    Optional<StorefrontPage> findBySlug(String slug);
    Optional<StorefrontPage> findBySlugAndPublishedTrue(String slug);
    Optional<StorefrontPage> findByTenantIdAndSlugAndPublishedTrue(String tenantId, String slug);
    boolean existsBySlug(String slug);
}
