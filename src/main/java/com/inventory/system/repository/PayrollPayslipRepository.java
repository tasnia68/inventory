package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollPayslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollPayslipRepository extends JpaRepository<PayrollPayslip, UUID> {
    List<PayrollPayslip> findAllByOrderByCreatedAtDesc();
    Optional<PayrollPayslip> findByPayrollRunItemId(UUID payrollRunItemId);
}
