package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AgingAnalysisReportDto {
    private UUID productVariantId;
    private String productName;
    private String sku;
    private UUID warehouseId;
    private String warehouseName;
    private BigDecimal onHandQuantity;
    private BigDecimal outboundQuantityLast30Days;
    private LocalDateTime lastMovementAt;
    private long daysSinceLastMovement;
    private String movementClass;
    private BigDecimal totalValue;
}