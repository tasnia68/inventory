package com.inventory.system.repository;

import com.inventory.system.common.entity.StockStatus;
import com.inventory.system.common.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID>, JpaSpecificationExecutor<Stock> {
    List<Stock> findByProductVariantIdAndWarehouseIdAndQuantityGreaterThan(UUID productVariantId, UUID warehouseId, BigDecimal quantity);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationId(UUID productVariantId, UUID warehouseId, UUID storageLocationId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNull(UUID productVariantId, UUID warehouseId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchId(UUID productVariantId, UUID warehouseId, UUID storageLocationId, UUID batchId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchId(UUID productVariantId, UUID warehouseId, UUID batchId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdIsNull(UUID productVariantId, UUID warehouseId, UUID storageLocationId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(UUID productVariantId, UUID warehouseId);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdAndStatus(UUID productVariantId, UUID warehouseId, UUID storageLocationId, UUID batchId, StockStatus status);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdAndStatus(UUID productVariantId, UUID warehouseId, UUID batchId, StockStatus status);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdIsNullAndStatus(UUID productVariantId, UUID warehouseId, UUID storageLocationId, StockStatus status);

    Optional<Stock> findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNullAndStatus(UUID productVariantId, UUID warehouseId, StockStatus status);

    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.productVariant.id = :productVariantId AND s.warehouse.id = :warehouseId AND s.status = 'AVAILABLE'")
    BigDecimal countTotalQuantityByProductVariantAndWarehouse(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);

        @Query("""
            SELECT SUM(s.quantity)
            FROM Stock s
            WHERE s.productVariant.id = :productVariantId
              AND s.warehouse.id = :warehouseId
              AND ((:storageLocationId IS NULL AND s.storageLocation IS NULL) OR s.storageLocation.id = :storageLocationId)
              AND ((:batchId IS NULL AND s.batch IS NULL) OR s.batch.id = :batchId)
                            AND s.status = 'AVAILABLE'
            """)
        BigDecimal countTotalQuantityByInventoryPosition(
            @Param("productVariantId") UUID productVariantId,
            @Param("warehouseId") UUID warehouseId,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("batchId") UUID batchId);

    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.productVariant.id = :productVariantId AND s.warehouse.id = :warehouseId AND s.status = :status")
    BigDecimal countTotalQuantityByProductVariantAndWarehouseAndStatus(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId, @Param("status") StockStatus status);

    @Query("select s from Stock s join s.productVariant pv join s.warehouse w " +
            "where lower(pv.sku) like lower(concat('%', :q, '%')) " +
            "or lower(w.name) like lower(concat('%', :q, '%'))")
    Page<Stock> searchByQuery(@Param("q") String query, Pageable pageable);
}
