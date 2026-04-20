package com.inventory.system.payload;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.ShipmentStatus;
import lombok.Data;

import java.math.BigDecimal;
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
    private ShipmentStatus status;
    private String carrier;
    private String courierProvider;
    private String courierService;
    private CourierDispatchStatus courierDispatchStatus;
    private String courierReference;
    private String trackingNumber;
    private String trackingUrl;
    private String shippingLabelUrl;
    private BigDecimal cashOnDeliveryAmount;
    private BigDecimal deliveryFee;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime pickupRequestedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime outForDeliveryAt;
    private String lastCourierEvent;
    private LocalDateTime lastCourierSyncAt;
    private String deliveryNote;
    private String notes;
    private List<ShipmentItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
