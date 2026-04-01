package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PayrollRunItemDto {
    private UUID id;
    private UUID employeePayrollProfileId;
    private String employeeCode;
    private String employeeName;
    private UUID employeeSalaryAssignmentId;
    private BigDecimal grossPay;
    private BigDecimal totalEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal statutoryDeductions;
    private BigDecimal netPay;
    private BigDecimal workingDays;
    private BigDecimal absentDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal manualAllowance;
    private BigDecimal manualDeduction;
    private String currency;
    private String notes;
}
