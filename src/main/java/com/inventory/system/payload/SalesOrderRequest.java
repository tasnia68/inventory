package com.inventory.system.payload;

import com.inventory.system.common.entity.OrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SalesOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private LocalDate expectedDeliveryDate;

    private OrderPriority priority;

    private String currency;

    private String notes;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<SalesOrderItemRequest> items;
}
