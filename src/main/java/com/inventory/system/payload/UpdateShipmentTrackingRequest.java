package com.inventory.system.payload;

import com.inventory.system.common.entity.ShipmentStatus;
import lombok.Data;

@Data
public class UpdateShipmentTrackingRequest {
    private String trackingNumber;
    private String trackingUrl;
    private ShipmentStatus status;
}