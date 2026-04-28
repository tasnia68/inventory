package com.inventory.system.payload;

import com.inventory.system.common.entity.DeliveryZone;
import lombok.Data;

import java.util.UUID;

@Data
public class ConfirmOrderRequest {
    private UUID courierProfileId;
    private DeliveryZone deliveryZone;
}
