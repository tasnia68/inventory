package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SupplierProductDto {
    private UUID id;
    private UUID supplierId;
    private String supplierName;
    private UUID productVariantId;
    private String productVariantName;
    private String supplierSku;
    private BigDecimal price;
    private String currency;
    private Integer leadTimeDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
