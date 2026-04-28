package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontShipmentDto {
    private String shipmentNumber;
    private String shipmentStatus;
    private String courierProvider;
    private String courierService;
    private String courierDispatchStatus;
    private String courierReference;
    private String trackingNumber;
    private String trackingUrl;
    private String expectedDeliveryDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime pickupRequestedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime outForDeliveryAt;
    private String lastCourierEvent;
    private List<StorefrontShipmentTimelineDto> timeline = new ArrayList<>();
}