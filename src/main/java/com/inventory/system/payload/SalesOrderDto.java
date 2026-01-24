package com.inventory.system.payload;

import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SalesOrderDto {
    private UUID id;
    private String soNumber;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private SalesOrderStatus status;
    private OrderPriority priority;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private List<SalesOrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
