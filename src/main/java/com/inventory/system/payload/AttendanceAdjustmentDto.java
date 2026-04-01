package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollInputSourceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AttendanceAdjustmentDto {
    private UUID id;
    private UUID employeePayrollProfileId;
    private String employeeCode;
    private String employeeName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate attendanceDate;
    private PayrollInputSourceType sourceType;
    private String sourceReference;
    private String deviceIdentifier;
    private Integer lateMinutes;
    private BigDecimal absentDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal manualAllowance;
    private BigDecimal manualDeduction;
    private String notes;
}
