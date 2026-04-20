package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorefrontDomainRequest {
    @NotBlank
    private String hostname;
}
