package com.inventory.system.repository;

import com.inventory.system.common.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE sm.productVariant.id = :productVariantId AND sm.warehouse.id = :warehouseId AND sm.createdAt >= :fromDate AND sm.type IN :types")
    BigDecimal sumQuantityByProductAndWarehouseAndDateAndType(
            @Param("productVariantId") UUID productVariantId,
            @Param("warehouseId") UUID warehouseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("types") List<StockMovement.StockMovementType> types);
}
