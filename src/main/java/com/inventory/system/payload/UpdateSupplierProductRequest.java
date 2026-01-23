package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSupplierProductRequest {
    private String supplierSku;
    private BigDecimal price;
    private String currency;
    private Integer leadTimeDays;
}
