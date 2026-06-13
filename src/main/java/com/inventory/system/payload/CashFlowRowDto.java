package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CashFlowRowDto {
    private String section;
    private String label;
    private BigDecimal amount;
}
