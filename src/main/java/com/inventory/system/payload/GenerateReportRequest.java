package com.inventory.system.payload;

import com.inventory.system.common.entity.ReportOutputFormat;
import com.inventory.system.common.entity.ReportType;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class GenerateReportRequest {
    private UUID configurationId;
    private ReportType reportType;
    private UUID warehouseId;
    private UUID productVariantId;
    private UUID supplierId;
    private UUID customerId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer slowMovingThresholdDays;
    private PurchaseOrderStatus purchaseOrderStatus;
    private SalesOrderStatus salesOrderStatus;
    private ReportOutputFormat format = ReportOutputFormat.CSV;
    private String search;
    private String sortBy;
    private String sortDirection;
    private Integer limit;
    private List<String> columns;
    private Map<String, String> fieldFilters;
}