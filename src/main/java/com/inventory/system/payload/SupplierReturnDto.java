package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierReturnStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierReturnDto {
    private UUID id;
    private String returnNumber;
    private UUID goodsReceiptNoteId;
    private String goodsReceiptNoteNumber;
    private UUID supplierId;
    private String supplierName;
    private UUID warehouseId;
    private String warehouseName;
    private SupplierReturnStatus status;
    private String reason;
    private String notes;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private List<SupplierReturnItemDto> items;
}