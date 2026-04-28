package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PartialDeliveryLineRequest {

    @NotNull
    private UUID itemId;

    private BigDecimal fulfilledQuantity;
    private BigDecimal returnedQuantity;
    private BigDecimal cancelledQuantity;
}
