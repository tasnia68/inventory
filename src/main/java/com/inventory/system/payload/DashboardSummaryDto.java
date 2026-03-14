package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DashboardSummaryDto {
    private UUID warehouseId;
    private LocalDateTime generatedAt;
    private BigDecimal totalOnHandQuantity;
    private BigDecimal totalAvailableQuantity;
    private BigDecimal totalInventoryValue;
    private BigDecimal inventoryTurnover;
    private long openPurchaseOrders;
    private long openSalesOrders;
    private BigDecimal orderFulfillmentRate;
    private long stockOutIncidents;
    private long activeStockAlerts;
    private BigDecimal averageWarehouseUtilization;
}