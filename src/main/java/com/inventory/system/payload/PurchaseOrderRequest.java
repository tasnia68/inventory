package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseOrderRequest {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    private LocalDate expectedDeliveryDate;

    private String currency;

    private String notes;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
