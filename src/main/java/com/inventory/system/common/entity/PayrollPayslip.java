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

import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_payslips", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"payroll_run_item_id", "tenant_id"}),
        @UniqueConstraint(columnNames = {"payslip_number", "tenant_id"})
})
@Getter
@Setter
public class PayrollPayslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_item_id", nullable = false)
    private PayrollRunItem payrollRunItem;

    @Column(name = "payslip_number", nullable = false)
    private String payslipNumber;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private boolean published = true;
}
