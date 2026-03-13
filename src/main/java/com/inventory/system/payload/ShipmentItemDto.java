package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ShipmentItemDto {
    private UUID id;
    private UUID salesOrderItemId;
    private UUID productVariantId;
    private String sku;
    private BigDecimal quantity;
}