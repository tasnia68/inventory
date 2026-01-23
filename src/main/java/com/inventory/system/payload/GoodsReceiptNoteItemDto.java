package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class GoodsReceiptNoteItemDto {
    private UUID id;
    private UUID purchaseOrderItemId;
    private UUID productVariantId;
    private String productVariantSku;
    private Integer receivedQuantity;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;
    private String rejectionReason;
}
