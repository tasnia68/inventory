package com.inventory.system.repository;

import com.inventory.system.common.entity.InventoryValuationLayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryValuationLayerRepository extends JpaRepository<InventoryValuationLayer, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryValuationLayer l WHERE l.productVariant.id = :productVariantId AND l.warehouse.id = :warehouseId AND l.quantityRemaining > 0 ORDER BY l.receivedDate ASC")
    List<InventoryValuationLayer> findLayersForFifo(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryValuationLayer l WHERE l.productVariant.id = :productVariantId AND l.warehouse.id = :warehouseId AND l.quantityRemaining > 0 ORDER BY l.receivedDate DESC")
    List<InventoryValuationLayer> findLayersForLifo(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);

    @Query("SELECT SUM(l.quantityRemaining * l.unitCost) FROM InventoryValuationLayer l WHERE l.productVariant.id = :productVariantId AND l.warehouse.id = :warehouseId")
    BigDecimal calculateTotalValue(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);
}
