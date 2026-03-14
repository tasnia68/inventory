package com.inventory.system.payload;

import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageReasonCode;
import com.inventory.system.common.entity.DamageRecordSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateDamageRecordRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private UUID quarantineLocationId;

    @NotNull(message = "Damage source type is required")
    private DamageRecordSourceType sourceType;

    @NotNull(message = "Damage reason code is required")
    private DamageReasonCode reasonCode;

    private String reference;
    private String notes;

    @NotEmpty(message = "Damage items are required")
    @Valid
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "Product variant ID is required")
        private UUID productVariantId;

        private UUID batchId;
        private UUID sourceStorageLocationId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        private BigDecimal quantity;

        @NotNull(message = "Disposition is required")
        private DamageDispositionType disposition;

        private List<String> serialNumbers;
    }
}