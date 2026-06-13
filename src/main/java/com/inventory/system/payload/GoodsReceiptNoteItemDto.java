package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
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
    private Integer returnedQuantity;
    private String rejectionReason;

    private Boolean batchTracked;
    private Boolean serialTracked;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private UUID batchId;
    private List<String> serialNumbers;
}
