package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerStatus;
import com.inventory.system.common.entity.CustomerCategory;
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
    private BigDecimal outstandingBalance;
    private BigDecimal availableCredit;
    private BigDecimal storeCreditBalance;
    private CustomerCategory category;
    private Boolean isActive;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
