package com.inventory.system.repository;

import com.inventory.system.common.entity.PosShiftTenderCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosShiftTenderCountRepository extends JpaRepository<PosShiftTenderCount, UUID> {

    List<PosShiftTenderCount> findByShiftIdOrderByPaymentMethodAsc(UUID shiftId);

    void deleteByShiftId(UUID shiftId);
}