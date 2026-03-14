package com.inventory.system.repository;

import com.inventory.system.common.entity.PosShift;
import com.inventory.system.common.entity.PosShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosShiftRepository extends JpaRepository<PosShift, UUID> {
    Optional<PosShift> findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(UUID terminalId, PosShiftStatus status);
    Optional<PosShift> findFirstByCashierIdAndStatusOrderByOpenedAtDesc(UUID cashierId, PosShiftStatus status);
    List<PosShift> findByTerminalIdOrderByOpenedAtDesc(UUID terminalId);
    List<PosShift> findByOpenedAtBetweenOrderByOpenedAtDesc(LocalDateTime from, LocalDateTime to);
    List<PosShift> findByTerminalIdAndOpenedAtBetweenOrderByOpenedAtDesc(UUID terminalId, LocalDateTime from, LocalDateTime to);
}