package com.inventory.system.payload;

import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class SalesOrderSearchRequest {
    private UUID customerId;
    private UUID warehouseId;
    private SalesOrderStatus status;
    private OrderPriority priority;
    private String soNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private int page = 0;
    private int size = 10;
    private String sortBy = "orderDate";
    private String sortDirection = "desc";
}
