package com.inventory.system.payload;

import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageReasonCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateDamageRecordFromGrnRequest {

    private UUID quarantineLocationId;

    private Boolean createSupplierClaim = false;

    private String supplierClaimReason;

    private String supplierClaimNotes;

    @NotNull(message = "Damage reason code is required")
    private DamageReasonCode reasonCode;

    private String notes;

    @NotEmpty(message = "Damage items are required")
    @Valid
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "Goods receipt item ID is required")
        private UUID goodsReceiptNoteItemId;

        @NotNull(message = "Disposition is required")
        private DamageDispositionType disposition;

        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        private BigDecimal quantity;
    }
}