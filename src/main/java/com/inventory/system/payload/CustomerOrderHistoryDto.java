package com.inventory.system.payload;

import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CustomerOrderHistoryDto {
    private UUID salesOrderId;
    private String soNumber;
    private LocalDateTime orderDate;
    private SalesOrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private UUID warehouseId;
    private String warehouseName;
}