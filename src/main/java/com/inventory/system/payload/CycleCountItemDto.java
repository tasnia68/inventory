package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CycleCountItemDto {
    private UUID id;
    private UUID cycleCountId;
    private UUID productVariantId;
    private String productVariantSku;
    private String productVariantName;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private BigDecimal systemQuantity;
    private BigDecimal countedQuantity;
    private BigDecimal variance;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
