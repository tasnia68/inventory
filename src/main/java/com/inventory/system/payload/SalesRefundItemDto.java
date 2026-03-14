package com.inventory.system.payload;

import com.inventory.system.common.entity.ReturnDisposition;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class SalesRefundItemDto {
    private UUID id;
    private UUID salesOrderItemId;
    private UUID productVariantId;
    private String productVariantSku;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal refundAmount;
    private ReturnDisposition returnDisposition;
    private String reason;
    private UUID batchId;
    private String batchNumber;
    private UUID storageLocationId;
    private String storageLocationName;
    private List<String> serialNumbers;
}