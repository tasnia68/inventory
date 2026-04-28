package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourierProfileRequest {

    @NotBlank
    private String providerCode;

    @NotBlank
    private String displayName;

    private boolean isDefault;
    private boolean isActive = true;

    // JSON-encoded strings
    private String credentialsJson;
    private String configJson;
}
