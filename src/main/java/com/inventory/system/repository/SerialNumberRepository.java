package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.SerialNumberStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, UUID> {
    Optional<SerialNumber> findBySerialNumberAndProductVariantId(String serialNumber, UUID productVariantId);
    List<SerialNumber> findByProductVariantAndStatus(ProductVariant productVariant, SerialNumberStatus status);
    List<SerialNumber> findAllBySerialNumber(String serialNumber);
    List<SerialNumber> findByProductVariantId(UUID productVariantId);

        @Query("""
                        select s from SerialNumber s
                        where s.productVariant.id = :productVariantId
                            and s.warehouse.id = :warehouseId
                            and ((:storageLocationId is null and s.storageLocation is null) or s.storageLocation.id = :storageLocationId)
                            and ((:batchId is null and s.batch is null) or s.batch.id = :batchId)
                            and s.status = :status
                        order by s.serialNumber asc
                        """)
        List<SerialNumber> findByInventoryPosition(
                        @Param("productVariantId") UUID productVariantId,
                        @Param("warehouseId") UUID warehouseId,
                        @Param("storageLocationId") UUID storageLocationId,
                        @Param("batchId") UUID batchId,
                        @Param("status") SerialNumberStatus status);
}
