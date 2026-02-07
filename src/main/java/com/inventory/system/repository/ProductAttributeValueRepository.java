package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    List<ProductAttributeValue> findByVariantId(UUID variantId);
    List<ProductAttributeValue> findByAttributeId(UUID attributeId);
}
