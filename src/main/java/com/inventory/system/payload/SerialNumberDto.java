package com.inventory.system.payload;

import com.inventory.system.common.entity.SerialNumberStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SerialNumberDto {
    private UUID id;
    private String serialNumber;
    private UUID productVariantId;
    private String productVariantSku;
    private UUID warehouseId;
    private String warehouseName;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private SerialNumberStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
