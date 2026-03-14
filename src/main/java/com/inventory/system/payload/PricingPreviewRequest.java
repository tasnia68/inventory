package com.inventory.system.payload;

import com.inventory.system.common.entity.SalesChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class PricingPreviewRequest {
    private UUID customerId;

    @NotNull
    private UUID warehouseId;

    private UUID terminalId;

    @NotNull
    private SalesChannel salesChannel;

    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    private List<String> couponCodes = new ArrayList<>();

    @Valid
    @NotEmpty
    private List<PricingPreviewItemRequest> items;
}