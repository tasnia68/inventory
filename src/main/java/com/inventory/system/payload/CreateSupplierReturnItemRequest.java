package com.inventory.system.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateSupplierReturnItemRequest {
    @NotNull(message = "Goods receipt note item ID is required")
    private UUID goodsReceiptNoteItemId;

    @NotNull(message = "Return quantity is required")
    @Min(value = 1, message = "Return quantity must be at least 1")
    private Integer quantity;

    private String reason;
}