package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollDepartmentRepository extends JpaRepository<PayrollDepartment, UUID> {
    Optional<PayrollDepartment> findByName(String name);
}
