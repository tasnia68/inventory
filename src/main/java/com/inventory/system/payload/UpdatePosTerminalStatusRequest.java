package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePosTerminalStatusRequest {

    @NotNull
    private Boolean active;

    private String notes;
}
