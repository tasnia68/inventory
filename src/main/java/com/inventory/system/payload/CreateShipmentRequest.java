package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateShipmentRequest {

    @NotNull(message = "Sales Order ID is required")
    private UUID salesOrderId;

    private String carrier;
    private String trackingNumber;
    private LocalDate estimatedDeliveryDate;
    private String notes;

    @Valid
    private List<ShipmentItemRequest> items;

    @Data
    public static class ShipmentItemRequest {
        @NotNull(message = "Sales Order Item ID is required")
        private UUID salesOrderItemId;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
    }
}
