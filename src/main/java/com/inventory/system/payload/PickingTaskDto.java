package com.inventory.system.payload;

import com.inventory.system.common.entity.PickingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PickingTaskDto {
    private UUID id;
    private UUID salesOrderItemId;
    private UUID productVariantId;
    private String productVariantName;
    private String sku;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private BigDecimal requestedQuantity;
    private BigDecimal pickedQuantity;
    private PickingStatus status;
    private String notes;
}
