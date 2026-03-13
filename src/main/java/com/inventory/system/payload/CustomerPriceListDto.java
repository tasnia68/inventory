package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CustomerPriceListDto {
    private UUID id;
    private UUID customerId;
    private UUID productVariantId;
    private String sku;
    private BigDecimal price;
    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}