package com.inventory.system.repository;

import com.inventory.system.common.entity.EmployeePayrollProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePayrollProfileRepository extends JpaRepository<EmployeePayrollProfile, UUID> {
    Optional<EmployeePayrollProfile> findByUserId(UUID userId);
    List<EmployeePayrollProfile> findAllByActiveTrueOrderByCreatedAtDesc();
}
