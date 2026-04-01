package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollDesignation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollDesignationRepository extends JpaRepository<PayrollDesignation, UUID> {
    Optional<PayrollDesignation> findByName(String name);
}
