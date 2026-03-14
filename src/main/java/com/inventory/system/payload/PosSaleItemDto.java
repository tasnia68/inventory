package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PosSaleItemDto {
    private UUID id;
    private UUID productVariantId;
    private String sku;
    private String barcode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineDiscount;
    private BigDecimal lineTotal;
}