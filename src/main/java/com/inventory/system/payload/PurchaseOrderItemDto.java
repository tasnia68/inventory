package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseOrderItemDto {

    private UUID id;
    private UUID productVariantId;
    private String productVariantName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer receivedQuantity;
}
