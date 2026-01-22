package com.inventory.system.payload;

import com.inventory.system.common.entity.CycleCountStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateCycleCountRequest {
    private CycleCountStatus status;
    private UUID assignedUserId;
    private LocalDate dueDate;
    private String description;
}
