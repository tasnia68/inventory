package com.inventory.system.repository;

import com.inventory.system.common.entity.TreasuryReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TreasuryReconciliationRepository extends JpaRepository<TreasuryReconciliation, UUID>, JpaSpecificationExecutor<TreasuryReconciliation> {
}
