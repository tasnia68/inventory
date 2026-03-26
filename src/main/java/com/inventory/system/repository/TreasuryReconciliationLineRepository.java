package com.inventory.system.repository;

import com.inventory.system.common.entity.TreasuryReconciliationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TreasuryReconciliationLineRepository extends JpaRepository<TreasuryReconciliationLine, UUID> {
}
