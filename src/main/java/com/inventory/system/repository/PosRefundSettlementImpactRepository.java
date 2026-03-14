package com.inventory.system.repository;

import com.inventory.system.common.entity.PosRefundSettlementImpact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosRefundSettlementImpactRepository extends JpaRepository<PosRefundSettlementImpact, UUID> {

    List<PosRefundSettlementImpact> findByShiftIdOrderByOccurredAtDesc(UUID shiftId);
}