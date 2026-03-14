package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierClaimStatus;
import com.inventory.system.common.entity.SupplierClaimType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierClaimDto {
    private UUID id;
    private String claimNumber;
    private UUID goodsReceiptNoteId;
    private String goodsReceiptNoteNumber;
    private UUID supplierId;
    private String supplierName;
    private UUID warehouseId;
    private String warehouseName;
    private UUID damageRecordId;
    private String damageRecordNumber;
    private UUID supplierReturnId;
    private String supplierReturnNumber;
    private SupplierClaimStatus status;
    private SupplierClaimType claimType;
    private String reason;
    private String notes;
    private LocalDateTime claimedAt;
    private LocalDateTime resolvedAt;
    private List<SupplierClaimItemDto> items;
}