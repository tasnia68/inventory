package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WarehouseDto {
    private UUID id;
    private String name;
    private String location;
    private String type;
    private String contactNumber;
    private Boolean isActive;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
