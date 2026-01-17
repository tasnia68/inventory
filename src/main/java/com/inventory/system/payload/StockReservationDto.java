package com.inventory.system.payload;

import com.inventory.system.common.entity.ReservationPriority;
import com.inventory.system.common.entity.StockReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class StockReservationDto {
    private UUID id;
    private UUID productVariantId;
    private String productVariantSku;
    private UUID warehouseId;
    private String warehouseName;
    private UUID storageLocationId;
    private String storageLocationName;
    private UUID batchId;
    private String batchNumber;
    private BigDecimal quantity;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private StockReservationStatus status;
    private ReservationPriority priority;
    private String referenceId;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
}
