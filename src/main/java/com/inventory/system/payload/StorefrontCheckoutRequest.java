package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class StorefrontCheckoutRequest {
    private UUID customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String contactName;
    private String customerEmail;
    private String customerPhoneNumber;
    private String shippingAddress;
    private UUID warehouseId;
    private String currency;
    private String notes;
    private LocalDate expectedDeliveryDate;
    private List<String> couponCodes = new ArrayList<>();

    @NotEmpty(message = "Checkout items cannot be empty")
    @Valid
    private List<StorefrontCartItemRequest> items = new ArrayList<>();
}
