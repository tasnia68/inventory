package com.inventory.system.repository;

import com.inventory.system.common.entity.CycleCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CycleCountRepository extends JpaRepository<CycleCount, UUID>, JpaSpecificationExecutor<CycleCount> {
    boolean existsByReference(String reference);
}
