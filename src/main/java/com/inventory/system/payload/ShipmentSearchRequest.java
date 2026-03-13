package com.inventory.system.payload;

import com.inventory.system.common.entity.ShipmentStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ShipmentSearchRequest {
    private UUID salesOrderId;
    private ShipmentStatus status;
    private String shipmentNumber;
    private String trackingNumber;
    private int page = 0;
    private int size = 10;
    private String sortBy = "shippedDate";
    private String sortDirection = "desc";
}