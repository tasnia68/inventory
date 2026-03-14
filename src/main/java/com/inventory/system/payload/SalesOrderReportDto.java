package com.inventory.system.payload;

import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SalesOrderReportDto {
    private UUID salesOrderId;
    private String soNumber;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private SalesOrderStatus status;
    private Integer itemCount;
    private BigDecimal orderedQuantity;
    private BigDecimal shippedQuantity;
    private BigDecimal fulfillmentRate;
    private BigDecimal totalAmount;
    private String currency;
}