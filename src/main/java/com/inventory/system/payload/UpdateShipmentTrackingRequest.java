package com.inventory.system.payload;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.ShipmentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateShipmentTrackingRequest {
    private String carrier;
    private String courierProvider;
    private String courierService;
    private String courierReference;
    private CourierDispatchStatus courierDispatchStatus;
    private String trackingNumber;
    private String trackingUrl;
    private BigDecimal cashOnDeliveryAmount;
    private BigDecimal deliveryFee;
    private String lastCourierEvent;
    private LocalDateTime lastCourierSyncAt;
    private LocalDateTime pickupRequestedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime outForDeliveryAt;
    private ShipmentStatus status;
}
