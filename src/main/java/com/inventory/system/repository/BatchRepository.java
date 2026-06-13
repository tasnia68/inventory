package com.inventory.system.repository;

import com.inventory.system.common.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByBatchNumberAndProductVariantId(String batchNumber, UUID productVariantId);
    List<Batch> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);
    List<Batch> findByExpiryDateBefore(LocalDate date);
    List<Batch> findByProductVariantId(UUID productVariantId);

    /**
     * FEFO (First-Expiry-First-Out) lookup. Returns batches with available stock in the given
     * warehouse, sorted by expiry date ascending (NULLs last). Includes only AVAILABLE stock.
     */
    @Query("""
        select b from Batch b
        where b.productVariant.id = :productVariantId
          and exists (
              select 1 from Stock s
              where s.batch = b
                and s.warehouse.id = :warehouseId
                and s.status = com.inventory.system.common.entity.StockStatus.AVAILABLE
                and s.quantity > 0
          )
          and (b.expiryDate is null or b.expiryDate >= :asOfDate)
        order by case when b.expiryDate is null then 1 else 0 end, b.expiryDate asc, b.createdAt asc
        """)
    List<Batch> findAvailableForFefo(
        @Param("productVariantId") UUID productVariantId,
        @Param("warehouseId") UUID warehouseId,
        @Param("asOfDate") LocalDate asOfDate);
}
