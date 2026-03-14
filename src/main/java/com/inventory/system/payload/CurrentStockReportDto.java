package com.inventory.system.payload;

import com.inventory.system.common.entity.StockStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CurrentStockReportDto {
    private UUID productVariantId;
    private String productName;
    private String sku;
    private UUID warehouseId;
    private String warehouseName;
    private StockStatus stockStatus;
    private BigDecimal onHandQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalValue;
    private String currency;
}