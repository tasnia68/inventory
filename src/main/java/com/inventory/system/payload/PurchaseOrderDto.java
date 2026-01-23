package com.inventory.system.payload;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseOrderDto {

    private UUID id;
    private String poNumber;
    private UUID supplierId;
    private String supplierName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private List<PurchaseOrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
