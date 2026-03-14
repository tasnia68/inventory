package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SupplierPerformanceReportDto {
    private UUID supplierId;
    private String supplierName;
    private Double supplierRating;
    private long purchaseOrderCount;
    private long completedOrderCount;
    private BigDecimal totalSpend;
    private BigDecimal fulfillmentRate;
    private BigDecimal onTimeDeliveryRate;
    private BigDecimal averageLeadTimeDays;
}