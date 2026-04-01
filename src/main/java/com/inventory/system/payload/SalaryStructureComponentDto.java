package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollComponentType;
import com.inventory.system.common.entity.PayrollComponentValueType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SalaryStructureComponentDto {
    private UUID id;
    private UUID payrollComponentId;
    private String payrollComponentCode;
    private String payrollComponentName;
    private PayrollComponentType componentType;
    private PayrollComponentValueType valueType;
    private BigDecimal amount;
    private BigDecimal rate;
    private Integer sortOrder;
}
