package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosKpiDto {
    private BigDecimal grossSales;
    private long ticketCount;
    private BigDecimal unitsSold;
    private BigDecimal averageTicket;
}