package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierClaimType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateSupplierClaimRequest {
    private UUID damageRecordId;

    private SupplierClaimType claimType;

    private String reason;

    private String notes;

    @NotEmpty(message = "Supplier claim items are required")
    @Valid
    private List<CreateSupplierClaimItemRequest> items;
}