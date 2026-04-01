package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollPaymentRepository extends JpaRepository<PayrollPayment, UUID> {
    List<PayrollPayment> findByPayrollRunIdOrderByPaymentDateDesc(UUID payrollRunId);
}
