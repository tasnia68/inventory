package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSuspendedPosSaleRequest {

    @NotNull
    private UUID terminalId;

    private UUID customerId;

    @NotNull
    private UUID warehouseId;

    @DecimalMin(value = "0.0")
    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    private String currency;
    private String notes;
    private List<String> couponCodes = new ArrayList<>();

    @Valid
    @NotEmpty
    private List<SuspendedPosSaleItemRequest> items;
}