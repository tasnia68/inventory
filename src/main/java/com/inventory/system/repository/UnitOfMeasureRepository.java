package com.inventory.system.repository;

import com.inventory.system.common.entity.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {
    Optional<UnitOfMeasure> findByCategoryAndIsBaseTrue(UnitOfMeasure.UomCategory category);
    List<UnitOfMeasure> findByCategory(UnitOfMeasure.UomCategory category);
}
