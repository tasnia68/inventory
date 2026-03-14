package com.inventory.system.payload;

import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageReasonCode;
import com.inventory.system.common.entity.DamageRecordSourceType;
import com.inventory.system.common.entity.DamageRecordStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DamageRecordReportDto {
    private UUID damageRecordId;
    private String recordNumber;
    private DamageRecordStatus status;
    private DamageRecordSourceType sourceType;
    private DamageReasonCode reasonCode;
    private UUID warehouseId;
    private String warehouseName;
    private UUID productVariantId;
    private String productVariantSku;
    private DamageDispositionType disposition;
    private BigDecimal quantity;
    private LocalDateTime damageDate;
}