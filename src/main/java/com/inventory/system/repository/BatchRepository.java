package com.inventory.system.repository;

import com.inventory.system.common.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByBatchNumberAndProductVariantId(String batchNumber, UUID productVariantId);
    List<Batch> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);
}
