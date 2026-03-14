package com.inventory.system.payload;

import com.inventory.system.common.entity.RefundMethod;
import com.inventory.system.common.entity.SalesRefundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateSalesRefundRequest {

    @NotNull(message = "Sales order ID is required")
    private UUID salesOrderId;

    private UUID rmaId;
    private UUID warehouseId;
    private SalesRefundType refundType;

    @NotNull(message = "Refund method is required")
    private RefundMethod refundMethod;

    private String reason;
    private String notes;

    @NotEmpty(message = "Refund items are required")
    @Valid
    private List<CreateSalesRefundItemRequest> items;

    @Valid
    private List<CreateExchangeReplacementItemRequest> replacementItems;
}