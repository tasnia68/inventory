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

@Entity
@Table(name = "payroll_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id"})
})
@Getter
@Setter
public class PayrollSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_expense_account_id")
    private ChartOfAccount salaryExpenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allowance_expense_account_id")
    private ChartOfAccount allowanceExpenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deduction_liability_account_id")
    private ChartOfAccount deductionLiabilityAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_payable_account_id")
    private ChartOfAccount payrollPayableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_clearing_account_id")
    private ChartOfAccount cashClearingAccount;

    @Column(name = "monthly_work_days", nullable = false)
    private Integer monthlyWorkDays = 30;

    @Column(name = "weekly_work_days", nullable = false)
    private Integer weeklyWorkDays = 7;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "BDT";
}
