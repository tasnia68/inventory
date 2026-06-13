package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PosSaleItemDto {
    private UUID id;
    private UUID productVariantId;
    private String sku;
    private String barcode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineDiscount;
    private BigDecimal lineTotal;
    private UUID batchId;
    private String batchNumber;
    private LocalDate batchExpiryDate;
    private List<String> serialNumbers;
}