package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class BulkProductOperationRequest {
    private List<UUID> productVariantIds;
    private BigDecimal percentagePriceAdjustment;
    private BigDecimal absolutePriceAdjustment;
    private Boolean active;
}