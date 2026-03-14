package com.inventory.system.payload;

import com.inventory.system.common.entity.DamageDispositionType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DamageRecordItemDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantSku;
    private UUID batchId;
    private String batchNumber;
    private UUID sourceStorageLocationId;
    private String sourceStorageLocationName;
    private BigDecimal quantity;
    private DamageDispositionType disposition;
    private List<String> serialNumbers;
}