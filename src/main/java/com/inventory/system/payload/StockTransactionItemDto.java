package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockTransactionItemDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantSku;
    private BigDecimal quantity;
    private UUID sourceStorageLocationId;
    private String sourceStorageLocationName;
    private UUID destinationStorageLocationId;
    private String destinationStorageLocationName;
}
