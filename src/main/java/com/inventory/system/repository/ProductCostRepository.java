package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductCost;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ProductCostRepository extends JpaRepository<ProductCost, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductCost> findByProductVariantIdAndWarehouseId(UUID productVariantId, UUID warehouseId);
}
