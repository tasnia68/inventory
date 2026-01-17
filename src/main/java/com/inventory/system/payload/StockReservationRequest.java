package com.inventory.system.payload;

import com.inventory.system.common.entity.ReservationPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class StockReservationRequest {

    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private UUID storageLocationId;

    private UUID batchId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private LocalDateTime expiresAt;

    private ReservationPriority priority = ReservationPriority.MEDIUM;

    private String referenceId;

    private String notes;
}
