package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_run_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"payroll_run_id", "employee_payroll_profile_id", "tenant_id"})
})
@Getter
@Setter
public class PayrollRunItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_payroll_profile_id", nullable = false)
    private EmployeePayrollProfile employeePayrollProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_salary_assignment_id")
    private EmployeeSalaryAssignment employeeSalaryAssignment;

    @Column(name = "gross_pay", nullable = false, precision = 19, scale = 6)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Column(name = "total_earnings", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "statutory_deductions", nullable = false, precision = 19, scale = 6)
    private BigDecimal statutoryDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 19, scale = 6)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Column(name = "working_days", nullable = false, precision = 12, scale = 3)
    private BigDecimal workingDays = BigDecimal.ZERO;

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

    @Column(nullable = false, length = 3)
    private String currency = "BDT";

    @Column(columnDefinition = "TEXT")
    private String notes;
}
