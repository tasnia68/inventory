package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, UUID> {
    Optional<PayrollComponent> findByCode(String code);
}
