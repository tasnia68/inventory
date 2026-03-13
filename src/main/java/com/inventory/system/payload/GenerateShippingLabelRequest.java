package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateShippingLabelRequest {
    private String serviceLevel;
    private BigDecimal packageWeight;
}