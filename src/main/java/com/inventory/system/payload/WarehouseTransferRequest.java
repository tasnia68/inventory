package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class WarehouseTransferRequest {
    @NotNull
    private UUID sourceWarehouseId;
    @NotNull
    private UUID destinationWarehouseId;
    private String reference;
    private String notes;
    @Valid
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull
        private UUID productVariantId;
        @NotNull
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private UUID sourceStorageLocationId;
        private UUID destinationStorageLocationId;
    }
}
