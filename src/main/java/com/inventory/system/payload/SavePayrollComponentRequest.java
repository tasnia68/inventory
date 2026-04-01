package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollComponentType;
import com.inventory.system.common.entity.PayrollComponentValueType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SavePayrollComponentRequest {
    private String code;
    private String name;
    private PayrollComponentType componentType;
    private PayrollComponentValueType valueType;
    private BigDecimal defaultAmount;
    private BigDecimal defaultRate;
    private Boolean active;
    private Boolean statutory;
    private Boolean editable;
    private String description;
}
