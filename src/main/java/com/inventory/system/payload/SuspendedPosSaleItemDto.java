package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SuspendedPosSaleItemDto {
    private UUID id;
    private UUID productVariantId;
    private String sku;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineDiscount;
    private BigDecimal lineTotal;
}