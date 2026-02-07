package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductTemplateId(UUID productTemplateId);
}
