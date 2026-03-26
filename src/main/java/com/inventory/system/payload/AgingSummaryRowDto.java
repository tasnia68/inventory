package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class AgingSummaryRowDto {
    private UUID partyId;
    private String partyName;
    private long invoiceCount;
    private BigDecimal totalOpenAmount;
    private BigDecimal currentAmount;
    private BigDecimal days1To30Amount;
    private BigDecimal days31To60Amount;
    private BigDecimal days61To90Amount;
    private BigDecimal over90Amount;
}
