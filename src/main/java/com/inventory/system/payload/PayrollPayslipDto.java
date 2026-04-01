package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PayrollPayslipDto {
    private UUID id;
    private String payslipNumber;
    private UUID payrollRunId;
    private String payrollRunNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private UUID employeePayrollProfileId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal grossPay;
    private BigDecimal totalEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal statutoryDeductions;
    private BigDecimal netPay;
    private String currency;
    private LocalDateTime generatedAt;
    private boolean published;
}
