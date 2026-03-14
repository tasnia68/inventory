package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePosTerminalRequest {
    private String terminalCode;

    @NotBlank
    private String name;

    @NotNull
    private UUID warehouseId;

    private String notes;
}