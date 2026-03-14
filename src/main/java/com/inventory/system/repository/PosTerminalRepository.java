package com.inventory.system.repository;

import com.inventory.system.common.entity.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, UUID> {
    List<PosTerminal> findAllByOrderByNameAsc();
    List<PosTerminal> findByActiveTrueOrderByNameAsc();
    Optional<PosTerminal> findByTerminalCode(String terminalCode);
}