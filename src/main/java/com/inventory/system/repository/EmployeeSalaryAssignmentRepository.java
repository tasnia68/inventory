package com.inventory.system.repository;

import com.inventory.system.common.entity.EmployeeSalaryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeSalaryAssignmentRepository extends JpaRepository<EmployeeSalaryAssignment, UUID> {
    List<EmployeeSalaryAssignment> findByEmployeePayrollProfileIdOrderByEffectiveFromDesc(UUID employeePayrollProfileId);
    Optional<EmployeeSalaryAssignment> findFirstByEmployeePayrollProfileIdAndActiveTrueOrderByEffectiveFromDesc(UUID employeePayrollProfileId);
}
