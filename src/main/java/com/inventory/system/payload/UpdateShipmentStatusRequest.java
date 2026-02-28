package com.inventory.system.payload;

import com.inventory.system.common.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateShipmentStatusRequest {
    @NotNull(message = "Status is required")
    private ShipmentStatus status;

    private String notes;
}
