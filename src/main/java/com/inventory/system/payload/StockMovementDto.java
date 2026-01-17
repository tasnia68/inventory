package com.inventory.system.payload;

import com.inventory.system.common.entity.StockMovement.StockMovementType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class StockMovementDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantSku;
    private UUID warehouseId;
    private String warehouseName;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private StockMovementType type;
    private String reason;
    private String referenceId;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<String> serialNumbers;
}
