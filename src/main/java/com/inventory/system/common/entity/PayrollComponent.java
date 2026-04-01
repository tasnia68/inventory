package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_components", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
public class PayrollComponent extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false)
    private PayrollComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private PayrollComponentValueType valueType = PayrollComponentValueType.FIXED;

    @Column(name = "default_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal defaultAmount = BigDecimal.ZERO;

    @Column(name = "default_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal defaultRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean statutory = false;

    @Column(nullable = false)
    private boolean editable = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}
