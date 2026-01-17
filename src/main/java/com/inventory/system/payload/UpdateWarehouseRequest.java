package com.inventory.system.payload;

import lombok.Data;

@Data
public class UpdateWarehouseRequest {
    private String name;
    private String location;
    private String type;
    private String contactNumber;
    private Boolean isActive;
}
