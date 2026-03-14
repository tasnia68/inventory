package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class PosTerminalDto {
    private UUID id;
    private String terminalCode;
    private String name;
    private UUID warehouseId;
    private String warehouseName;
    private Boolean active;
    private String notes;
}