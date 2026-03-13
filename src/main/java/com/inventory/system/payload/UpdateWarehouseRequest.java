package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateWarehouseRequest {
    private String name;
    private String location;
    private String type;
    private String contactNumber;
    private Boolean isActive;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
}
