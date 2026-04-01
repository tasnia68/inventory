package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_adjustments")
@Getter
@Setter
public class AttendanceAdjustment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_payroll_profile_id", nullable = false)
    private EmployeePayrollProfile employeePayrollProfile;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private PayrollInputSourceType sourceType = PayrollInputSourceType.MANUAL;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "device_identifier")
    private String deviceIdentifier;

    @Column(name = "late_minutes", nullable = false)
    private Integer lateMinutes = 0;

    @Column(name = "absent_days", nullable = false, precision = 12, scale = 3)
    private BigDecimal absentDays = BigDecimal.ZERO;

    @Column(name = "leave_days", nullable = false, precision = 12, scale = 3)
    private BigDecimal leaveDays = BigDecimal.ZERO;

    @Column(name = "overtime_hours", nullable = false, precision = 12, scale = 3)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "manual_allowance", nullable = false, precision = 19, scale = 6)
    private BigDecimal manualAllowance = BigDecimal.ZERO;

    @Column(name = "manual_deduction", nullable = false, precision = 19, scale = 6)
    private BigDecimal manualDeduction = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
