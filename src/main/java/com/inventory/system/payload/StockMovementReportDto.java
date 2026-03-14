package com.inventory.system.payload;

import com.inventory.system.common.entity.StockMovement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StockMovementReportDto {
    private UUID movementId;
    private LocalDateTime movementDate;
    private UUID productVariantId;
    private String productName;
    private String sku;
    private UUID warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private StockMovement.StockMovementType type;
    private String reason;
    private String referenceId;
}