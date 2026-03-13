package com.inventory.system.payload;

import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRmaStatusRequest {

    @NotNull(message = "RMA status is required")
    private ReturnMerchandiseStatus status;

    private String notes;
}