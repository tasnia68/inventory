package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateRmaRequest {

    @NotNull(message = "Sales Order ID is required")
    private UUID salesOrderId;

    private String reason;
    private String notes;

    @NotEmpty(message = "Items are required")
    @Valid
    private List<ReturnItemRequest> items;

    @Data
    public static class ReturnItemRequest {
        @NotNull(message = "Sales Order Item ID is required")
        private UUID salesOrderItemId;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        private String condition;
        private String resolution;
    }
}
