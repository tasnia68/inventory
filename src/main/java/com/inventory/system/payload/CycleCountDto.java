package com.inventory.system.payload;

import com.inventory.system.common.entity.CycleCountStatus;
import com.inventory.system.common.entity.CycleCountType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CycleCountDto {
    private UUID id;
    private String reference;
    private UUID warehouseId;
    private String warehouseName;
    private CycleCountStatus status;
    private CycleCountType type;
    private LocalDate dueDate;
    private LocalDate completionDate;
    private String description;
    private UUID assignedUserId;
    private String assignedUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
