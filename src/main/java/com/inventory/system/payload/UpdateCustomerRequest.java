package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCustomerRequest {
    private String name;
    private String contactName;
    @Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
    private String address;
    private BigDecimal creditLimit;
    private Boolean isActive;
    private CustomerStatus status;
}
