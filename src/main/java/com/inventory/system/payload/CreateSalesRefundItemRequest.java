package com.inventory.system.payload;

import com.inventory.system.common.entity.ReturnDisposition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSalesRefundItemRequest {

    @NotNull(message = "Sales order item ID is required")
    private UUID salesOrderItemId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    @NotNull(message = "Return disposition is required")
    private ReturnDisposition returnDisposition;

    private String reason;
    private UUID batchId;
    private UUID storageLocationId;
    private List<String> serialNumbers;
}