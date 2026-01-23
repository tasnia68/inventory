package com.inventory.system.payload;

import com.inventory.system.common.entity.SupplierStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SupplierDto {
    private UUID id;
    private String name;
    private String contactName;
    private String email;
    private String phoneNumber;
    private String address;
    private String paymentTerms;
    private Boolean isActive;
    private Double rating;
    private SupplierStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
