package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateRmaRequest {

    @NotNull(message = "Sales order ID is required")
    private UUID salesOrderId;

    private UUID shipmentId;
    private String reason;
    private String notes;

    @NotEmpty(message = "RMA items cannot be empty")
    @Valid
    private List<CreateRmaItemRequest> items;
}