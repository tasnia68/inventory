package com.inventory.system.repository;

import com.inventory.system.common.entity.CycleCountItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CycleCountItemRepository extends JpaRepository<CycleCountItem, UUID>, JpaSpecificationExecutor<CycleCountItem> {
    List<CycleCountItem> findByCycleCountId(UUID cycleCountId);

    Optional<CycleCountItem> findByCycleCountIdAndProductVariantIdAndStorageLocationIdAndBatchId(
        UUID cycleCountId, UUID productVariantId, UUID storageLocationId, UUID batchId);

    Optional<CycleCountItem> findByCycleCountIdAndProductVariantIdAndStorageLocationIdIsNullAndBatchId(
        UUID cycleCountId, UUID productVariantId, UUID batchId);

    Optional<CycleCountItem> findByCycleCountIdAndProductVariantIdAndStorageLocationIdAndBatchIdIsNull(
        UUID cycleCountId, UUID productVariantId, UUID storageLocationId);

    Optional<CycleCountItem> findByCycleCountIdAndProductVariantIdAndStorageLocationIdIsNullAndBatchIdIsNull(
        UUID cycleCountId, UUID productVariantId);
}
