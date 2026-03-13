package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductVariantVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantVersionRepository extends JpaRepository<ProductVariantVersion, UUID> {
    Page<ProductVariantVersion> findByProductVariantId(UUID productVariantId, Pageable pageable);
    Optional<ProductVariantVersion> findTopByProductVariantIdOrderByVersionNumberDesc(UUID productVariantId);
}