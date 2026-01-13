package com.inventory.system.payload;

import com.inventory.system.common.entity.StorageLocation.StorageLocationType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StorageLocationDto {
    private UUID id;
    private String name;
    private StorageLocationType type;
    private UUID parentId;
    private UUID warehouseId;
    private String warehouseName;
    private List<StorageLocationDto> children;
}
