package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePickingTaskRequest {
    @NotNull(message = "Picked quantity is required")
    private BigDecimal pickedQuantity;
    private String notes;
}
