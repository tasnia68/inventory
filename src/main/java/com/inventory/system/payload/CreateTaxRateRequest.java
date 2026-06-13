package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateTaxRateRequest {
    private String code;
    private String name;
    private BigDecimal rate;
    private UUID outputAccountId;
    private UUID inputAccountId;
    private Boolean active;
}
