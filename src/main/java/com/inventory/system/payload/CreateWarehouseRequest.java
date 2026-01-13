package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String location;

    @NotBlank(message = "Warehouse type is required")
    private String type;

    private String contactNumber;

    private Boolean isActive = true;
}
