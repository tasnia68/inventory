package com.inventory.system.repository;

import com.inventory.system.common.entity.PosCashMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosCashMovementRepository extends JpaRepository<PosCashMovement, UUID> {

    List<PosCashMovement> findByShiftIdOrderByOccurredAtDesc(UUID shiftId);
}