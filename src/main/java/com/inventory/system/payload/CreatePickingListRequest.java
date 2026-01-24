package com.inventory.system.payload;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreatePickingListRequest {
    @NotEmpty(message = "At least one sales order ID is required")
    private List<UUID> salesOrderIds;
    private UUID assignedToId;
    private String notes;
}
