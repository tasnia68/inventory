package com.inventory.system.repository;

import com.inventory.system.common.entity.PosSuspendedSale;
import com.inventory.system.common.entity.PosSuspendedSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosSuspendedSaleRepository extends JpaRepository<PosSuspendedSale, UUID> {

    List<PosSuspendedSale> findByTerminalIdAndStatusOrderBySuspendedAtDesc(UUID terminalId, PosSuspendedSaleStatus status);

    Optional<PosSuspendedSale> findByIdAndStatus(UUID id, PosSuspendedSaleStatus status);
}