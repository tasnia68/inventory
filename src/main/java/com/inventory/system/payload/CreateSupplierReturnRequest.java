package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateSupplierReturnRequest {
    @NotNull(message = "Goods receipt note ID is required")
    private UUID goodsReceiptNoteId;

    private String reason;

    private String notes;

    @NotEmpty(message = "Supplier return items are required")
    @Valid
    private List<CreateSupplierReturnItemRequest> items;
}