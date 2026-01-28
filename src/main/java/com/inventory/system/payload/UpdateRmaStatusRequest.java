package com.inventory.system.payload;

import com.inventory.system.common.entity.RmaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRmaStatusRequest {
    @NotNull(message = "Status is required")
    private RmaStatus status;

    private String notes;
}
