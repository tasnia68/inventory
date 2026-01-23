package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateSupplierRequest {
    private String name;
    private String contactName;
    @Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
    private String address;
    private String paymentTerms;
    private Boolean isActive;
    private Double rating;
    private SupplierStatus status;
}
