package com.inventory.system.repository;

import com.inventory.system.common.entity.SupplierProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, UUID> {

    @EntityGraph(attributePaths = {"supplier", "productVariant"})
    List<SupplierProduct> findBySupplierId(UUID supplierId);

    @EntityGraph(attributePaths = {"supplier", "productVariant"})
    List<SupplierProduct> findByProductVariantId(UUID productVariantId);
}
