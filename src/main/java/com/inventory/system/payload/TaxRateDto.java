package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TaxRateDto {
    private UUID id;
    private String code;
    private String name;
    private BigDecimal rate;
    private UUID outputAccountId;
    private String outputAccountName;
    private UUID inputAccountId;
    private String inputAccountName;
    private boolean active;
}
