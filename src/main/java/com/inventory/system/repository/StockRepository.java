package com.inventory.system.repository;

import com.inventory.system.common.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID>, JpaSpecificationExecutor<Stock> {
    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationId(UUID productVariantId, UUID warehouseId, UUID storageLocationId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNull(UUID productVariantId, UUID warehouseId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchId(UUID productVariantId, UUID warehouseId, UUID storageLocationId, UUID batchId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchId(UUID productVariantId, UUID warehouseId, UUID batchId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdIsNull(UUID productVariantId, UUID warehouseId, UUID storageLocationId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(UUID productVariantId, UUID warehouseId);

    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.productVariant.id = :productVariantId AND s.warehouse.id = :warehouseId")
    BigDecimal countTotalQuantityByProductVariantAndWarehouse(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);
}
