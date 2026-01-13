package com.inventory.system.payload;

import com.inventory.system.common.entity.StorageLocation.StorageLocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateStorageLocationRequest {
    @NotBlank(message = "Location name is required")
    private String name;

    @NotNull(message = "Location type is required")
    private StorageLocationType type;

    private UUID parentId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
}
