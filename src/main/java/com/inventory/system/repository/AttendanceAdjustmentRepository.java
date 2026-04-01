package com.inventory.system.repository;

import com.inventory.system.common.entity.AttendanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceAdjustmentRepository extends JpaRepository<AttendanceAdjustment, UUID> {
    List<AttendanceAdjustment> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCreatedAtDesc(LocalDate periodStart, LocalDate periodEnd);
    List<AttendanceAdjustment> findByEmployeePayrollProfileIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(UUID employeePayrollProfileId, LocalDate periodStart, LocalDate periodEnd);
}
