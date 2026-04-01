package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdatePayrollRunItemRequest {
    private BigDecimal absentDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal manualAllowance;
    private BigDecimal manualDeduction;
    private String notes;
}
