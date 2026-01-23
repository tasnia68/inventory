package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateGoodsReceiptNoteRequest {
    @NotNull(message = "Purchase Order ID is required")
    private UUID purchaseOrderId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private String notes;
}
