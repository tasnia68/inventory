package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductAttribute;
import com.inventory.system.common.entity.ProductTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    List<ProductAttribute> findByTemplate(ProductTemplate template);
    List<ProductAttribute> findByTemplateId(UUID templateId);
}
