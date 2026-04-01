package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollRunItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollRunItemRepository extends JpaRepository<PayrollRunItem, UUID> {
    List<PayrollRunItem> findByPayrollRunIdOrderByCreatedAtAsc(UUID payrollRunId);
}
