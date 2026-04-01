package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollComponentType;
import com.inventory.system.common.entity.PayrollComponentValueType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PayrollComponentDto {
    private UUID id;
    private String code;
    private String name;
    private PayrollComponentType componentType;
    private PayrollComponentValueType valueType;
    private BigDecimal defaultAmount;
    private BigDecimal defaultRate;
    private boolean active;
    private boolean statutory;
    private boolean editable;
    private String description;
}
