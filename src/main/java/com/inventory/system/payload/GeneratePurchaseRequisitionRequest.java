package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GeneratePurchaseRequisitionRequest {
    @NotNull
    private UUID warehouseId;
    private String notes;
}
