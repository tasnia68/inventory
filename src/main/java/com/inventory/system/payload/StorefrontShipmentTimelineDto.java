package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontShipmentTimelineDto {
    private String eventType;
    private String summary;
    private String details;
    private LocalDateTime eventAt;
    private String shipmentStatus;
    private String courierDispatchStatus;
}