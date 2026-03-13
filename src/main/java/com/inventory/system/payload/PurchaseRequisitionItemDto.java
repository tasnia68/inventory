package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseRequisitionItemDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal suggestedQuantity;
}
