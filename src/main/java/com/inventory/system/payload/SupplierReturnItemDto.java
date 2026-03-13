package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SupplierReturnItemDto {
    private UUID id;
    private UUID goodsReceiptNoteItemId;
    private UUID productVariantId;
    private String productVariantSku;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private String reason;
}