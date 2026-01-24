package com.inventory.system.payload;

import com.inventory.system.common.entity.ShipmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ShipmentDto {
    private UUID id;
    private String shipmentNumber;
    private UUID salesOrderId;
    private String soNumber;
    private UUID warehouseId;
    private String warehouseName;
    private String carrier;
    private String trackingNumber;
    private ShipmentStatus status;
    private LocalDateTime shipmentDate;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime deliveryDate;
    private String notes;
    private List<ShipmentItemDto> items;
}
