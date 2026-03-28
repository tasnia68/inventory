package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorefrontOrderLookupRequest {
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    private String customerEmail;
    private String customerPhoneNumber;
}
