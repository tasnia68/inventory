package com.inventory.system.payload;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.DeliveryReviewStatus;
import com.inventory.system.common.entity.ShipmentStatus;
import com.inventory.system.common.entity.ShipmentTimelineEventType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ShipmentTimelineEventDto {
    private UUID id;
    private ShipmentTimelineEventType eventType;
    private String eventSource;
    private String summary;
    private String details;
    private LocalDateTime eventAt;
    private ShipmentStatus shipmentStatus;
    private CourierDispatchStatus courierDispatchStatus;
    private DeliveryReviewStatus deliveryReviewStatus;
    private boolean customerVisible;
}