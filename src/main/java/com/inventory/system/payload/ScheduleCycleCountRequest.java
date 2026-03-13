package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ScheduleCycleCountRequest {
    private LocalDate dueDate;
    private UUID assignedUserId;
    private String description;
}
