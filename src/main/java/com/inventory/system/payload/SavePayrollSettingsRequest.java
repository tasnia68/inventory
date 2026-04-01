package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SavePayrollSettingsRequest {
    private UUID salaryExpenseAccountId;
    private UUID allowanceExpenseAccountId;
    private UUID deductionLiabilityAccountId;
    private UUID payrollPayableAccountId;
    private UUID cashClearingAccountId;
    private Integer monthlyWorkDays;
    private Integer weeklyWorkDays;
    private String defaultCurrency;
}
