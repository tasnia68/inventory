package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CustomerDto {
    private UUID id;
    private String name;
    private String contactName;
    private String email;
    private String phoneNumber;
    private String address;
    private BigDecimal creditLimit;
    private Boolean isActive;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
