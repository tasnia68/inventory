package com.inventory.system.payload;

import com.inventory.system.common.entity.PickingStatus;
import com.inventory.system.common.entity.PickingType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PickingListDto {
    private UUID id;
    private String pickingNumber;
    private UUID warehouseId;
    private String warehouseName;
    private UUID assignedToId;
    private String assignedToName;
    private PickingStatus status;
    private PickingType type;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PickingTaskDto> tasks;
}
