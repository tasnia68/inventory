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
@Table(name = "salary_structure_components", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"salary_structure_id", "payroll_component_id", "tenant_id"})
})
@Getter
@Setter
public class SalaryStructureComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_component_id", nullable = false)
    private PayrollComponent payrollComponent;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
