package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSupplierRequest {
    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactName;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    private String address;

    private String paymentTerms;

    private Boolean isActive = true;

    private Double rating;

    private SupplierStatus status = SupplierStatus.PENDING;
}
