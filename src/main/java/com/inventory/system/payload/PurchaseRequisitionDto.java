package com.inventory.system.payload;

import com.inventory.system.common.entity.PurchaseRequisitionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseRequisitionDto {
    private UUID id;
    private String reference;
    private UUID warehouseId;
    private String warehouseName;
    private PurchaseRequisitionStatus status;
    private String notes;
    private LocalDateTime requestedAt;
    private List<PurchaseRequisitionItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
