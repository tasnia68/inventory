package com.inventory.system.payload;

import com.inventory.system.common.entity.StockStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StockDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantSku;
    private UUID warehouseId;
    private String warehouseName;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private StockStatus status;
    private BigDecimal quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
