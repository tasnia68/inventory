package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PosCatalogItemDto {
    private UUID id;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal onHand;

    private Boolean batchTracked;
    private Boolean serialTracked;
    private List<BatchOption> availableBatches;

    @Data
    public static class BatchOption {
        private UUID id;
        private String batchNumber;
        private LocalDate expiryDate;
        private BigDecimal availableQuantity;
    }
}
