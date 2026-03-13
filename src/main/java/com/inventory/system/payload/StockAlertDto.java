package com.inventory.system.payload;

import com.inventory.system.common.entity.StockAlertStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockAlertDto {
    private UUID productVariantId;
    private String productVariantName;
    private String sku;
    private UUID warehouseId;
    private String warehouseName;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private BigDecimal suggestedQuantity;
    private StockAlertStatus status;
}
