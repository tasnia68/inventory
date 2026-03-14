package com.inventory.system.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateSupplierClaimItemRequest {
    @NotNull(message = "Goods receipt note item ID is required")
    private UUID goodsReceiptNoteItemId;

    @NotNull(message = "Claim quantity is required")
    @Min(value = 1, message = "Claim quantity must be at least 1")
    private Integer quantity;

    private String reason;
}