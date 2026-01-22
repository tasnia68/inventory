package com.inventory.system.payload;

import com.inventory.system.common.entity.CycleCountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateCycleCountRequest {
    @NotNull
    private UUID warehouseId;
    @NotNull
    private CycleCountType type;
    private LocalDate dueDate;
    private String description;
    private UUID assignedUserId;
}
