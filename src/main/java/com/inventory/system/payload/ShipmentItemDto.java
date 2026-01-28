package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ShipmentItemDto {
    private UUID id;
    private UUID salesOrderItemId;
    private String productVariantName;
    private String productSku;
    private BigDecimal quantity;
}
