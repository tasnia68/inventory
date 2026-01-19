package com.inventory.system.repository;

import com.inventory.system.common.entity.StockReservation;
import com.inventory.system.common.entity.StockReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID>, JpaSpecificationExecutor<StockReservation> {

    @Query("SELECT SUM(r.quantity) FROM StockReservation r WHERE r.productVariant.id = :productVariantId AND r.warehouse.id = :warehouseId AND r.status IN ('ACTIVE', 'PENDING')")
    BigDecimal countTotalReservedQuantity(@Param("productVariantId") UUID productVariantId, @Param("warehouseId") UUID warehouseId);

    List<StockReservation> findByStatusAndExpiresAtBefore(StockReservationStatus status, LocalDateTime expiresAt);

    @Override
    @EntityGraph(attributePaths = {"productVariant", "warehouse", "storageLocation", "batch"})
    Page<StockReservation> findAll(Specification<StockReservation> spec, Pageable pageable);
}
