package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseCapacityUpdateRequest {
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
}
