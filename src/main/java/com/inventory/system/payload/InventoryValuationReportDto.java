package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InventoryValuationReportDto {
    private UUID productVariantId;
    private String productVariantSku;
    private String productName;
    private UUID warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalValue;
}
