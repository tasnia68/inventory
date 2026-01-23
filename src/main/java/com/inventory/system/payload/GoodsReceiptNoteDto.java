package com.inventory.system.payload;

import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class GoodsReceiptNoteDto {
    private UUID id;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String purchaseOrderNumber;
    private UUID supplierId;
    private String supplierName;
    private UUID warehouseId;
    private String warehouseName;
    private LocalDateTime receivedDate;
    private GoodsReceiptNoteStatus status;
    private String notes;
    private List<GoodsReceiptNoteItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
