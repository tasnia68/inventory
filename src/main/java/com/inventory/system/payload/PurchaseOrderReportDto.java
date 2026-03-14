package com.inventory.system.payload;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PurchaseOrderReportDto {
    private UUID purchaseOrderId;
    private String poNumber;
    private UUID supplierId;
    private String supplierName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private PurchaseOrderStatus status;
    private Integer itemCount;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal completionRate;
    private BigDecimal totalAmount;
    private String currency;
}