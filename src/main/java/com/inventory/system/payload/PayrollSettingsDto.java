package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PayrollSettingsDto {
    private UUID id;
    private UUID salaryExpenseAccountId;
    private String salaryExpenseAccountName;
    private UUID allowanceExpenseAccountId;
    private String allowanceExpenseAccountName;
    private UUID deductionLiabilityAccountId;
    private String deductionLiabilityAccountName;
    private UUID payrollPayableAccountId;
    private String payrollPayableAccountName;
    private UUID cashClearingAccountId;
    private String cashClearingAccountName;
    private Integer monthlyWorkDays;
    private Integer weeklyWorkDays;
    private String defaultCurrency;
}
