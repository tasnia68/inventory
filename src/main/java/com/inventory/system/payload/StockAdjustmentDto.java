package com.inventory.system.payload;

import com.inventory.system.common.entity.StockMovement.StockMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockAdjustmentDto {
    @NotNull(message = "Product Variant ID is required")
    private UUID productVariantId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private UUID storageLocationId;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    @NotNull(message = "Movement type is required")
    private StockMovementType type;

    private String reason;
    private String referenceId;
}
