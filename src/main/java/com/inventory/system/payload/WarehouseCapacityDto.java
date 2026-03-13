package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WarehouseCapacityDto {
    private UUID warehouseId;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
    private BigDecimal utilizationPercent;
}
