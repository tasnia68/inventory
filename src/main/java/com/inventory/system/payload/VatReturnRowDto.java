package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class VatReturnRowDto {
    private UUID taxRateId;
    private String code;
    private String name;
    private BigDecimal rate;
    private BigDecimal outputTax;
    private BigDecimal inputTax;
    private BigDecimal netTaxPayable;
}
