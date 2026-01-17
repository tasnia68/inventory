package com.inventory.system.repository;

import com.inventory.system.common.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID>, JpaSpecificationExecutor<Stock> {
    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationId(UUID productVariantId, UUID warehouseId, UUID storageLocationId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNull(UUID productVariantId, UUID warehouseId);
}
