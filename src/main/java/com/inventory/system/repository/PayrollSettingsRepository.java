package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollSettingsRepository extends JpaRepository<PayrollSettings, UUID> {
    Optional<PayrollSettings> findFirstByOrderByCreatedAtAsc();
}
