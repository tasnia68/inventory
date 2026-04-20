package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateShipmentRequest {

    @NotNull(message = "Sales order ID is required")
    private UUID salesOrderId;

    private String carrier;
    private String courierProvider;
    private String courierService;
    private String courierReference;
    private BigDecimal cashOnDeliveryAmount;
    private BigDecimal deliveryFee;
    private String notes;

    @NotEmpty(message = "Shipment items cannot be empty")
    @Valid
    private List<CreateShipmentItemRequest> items;
}
