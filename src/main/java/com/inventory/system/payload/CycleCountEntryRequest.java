package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CycleCountEntryRequest {
    @NotNull
    private UUID productVariantId;
    private UUID storageLocationId;
    private UUID batchId;
    @NotNull
    private BigDecimal countedQuantity;
    private List<String> serialNumbers;
    private String notes;
}
