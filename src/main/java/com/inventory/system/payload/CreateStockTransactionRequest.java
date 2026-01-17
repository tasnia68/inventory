package com.inventory.system.payload;

import com.inventory.system.common.entity.StockTransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateStockTransactionRequest {

    @NotNull(message = "Transaction type is required")
    private StockTransactionType type;

    private UUID sourceWarehouseId;
    private UUID destinationWarehouseId;

    private String reference;
    private String notes;

    @NotEmpty(message = "Transaction items cannot be empty")
    @Valid
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "Product variant ID is required")
        private UUID productVariantId;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        private BigDecimal unitCost;

        private UUID sourceStorageLocationId;
        private UUID destinationStorageLocationId;
    }
}
