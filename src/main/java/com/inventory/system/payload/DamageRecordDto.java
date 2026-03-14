package com.inventory.system.payload;

import com.inventory.system.common.entity.DamageReasonCode;
import com.inventory.system.common.entity.DamageRecordSourceType;
import com.inventory.system.common.entity.DamageRecordStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class DamageRecordDto {
    private UUID id;
    private String recordNumber;
    private DamageRecordStatus status;
    private DamageRecordSourceType sourceType;
    private DamageReasonCode reasonCode;
    private UUID warehouseId;
    private String warehouseName;
    private UUID quarantineLocationId;
    private String quarantineLocationName;
    private String reference;
    private String notes;
    private UUID supplierClaimId;
    private String supplierClaimNumber;
    private LocalDateTime damageDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<DamageRecordItemDto> items;
}