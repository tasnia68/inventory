package com.inventory.system.payload;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReplenishmentRuleDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantName;
    private UUID warehouseId;
    private String warehouseName;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private BigDecimal reorderQuantity;
    private BigDecimal safetyStock;
    private Integer leadTimeDays;
    private Boolean isEnabled;
}
