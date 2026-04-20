package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class StorefrontCartRequest {
    private UUID customerId;
    private String customerEmail;
    private String customerPhoneNumber;
    private UUID warehouseId;
    private String currency;
    private LocalDate expectedDeliveryDate;
    private List<String> couponCodes = new ArrayList<>();

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<StorefrontCartItemRequest> items = new ArrayList<>();
}
