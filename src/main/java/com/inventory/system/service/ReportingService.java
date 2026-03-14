package com.inventory.system.service;

import com.inventory.system.common.entity.ReportCategory;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.payload.AgingAnalysisReportDto;
import com.inventory.system.payload.CurrentStockReportDto;
import com.inventory.system.payload.DashboardSummaryDto;
import com.inventory.system.payload.DashboardWidgetDto;
import com.inventory.system.payload.DataExchangeDataset;
import com.inventory.system.payload.DataExchangeTemplateDto;
import com.inventory.system.payload.GenerateReportRequest;
import com.inventory.system.payload.GeneratedReportDto;
import com.inventory.system.payload.PurchaseOrderReportDto;
import com.inventory.system.payload.ReportFileDto;
import com.inventory.system.payload.ReportConfigurationDto;
import com.inventory.system.payload.ReportExecutionHistoryDto;
import com.inventory.system.payload.SalesOrderReportDto;
import com.inventory.system.payload.StockAlertDto;
import com.inventory.system.payload.StockMovementReportDto;
import com.inventory.system.payload.SupplierPerformanceReportDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportingService {
    List<ReportConfigurationDto> getConfigurations(ReportCategory category, Boolean active);

    ReportConfigurationDto getConfiguration(UUID id);

    ReportConfigurationDto createConfiguration(ReportConfigurationDto request);

    ReportConfigurationDto updateConfiguration(UUID id, ReportConfigurationDto request);

    void deleteConfiguration(UUID id);

    List<ReportExecutionHistoryDto> getExecutionHistory();

    List<CurrentStockReportDto> getCurrentStockReport(UUID warehouseId, UUID productVariantId);

    List<StockMovementReportDto> getStockMovementReport(UUID warehouseId, UUID productVariantId, LocalDate fromDate, LocalDate toDate);

    List<AgingAnalysisReportDto> getAgingAnalysisReport(UUID warehouseId, Integer slowMovingThresholdDays);

    List<PurchaseOrderReportDto> getPurchaseOrderReport(UUID supplierId, PurchaseOrderStatus status, LocalDate fromDate, LocalDate toDate);

    List<SalesOrderReportDto> getSalesOrderReport(UUID customerId, UUID warehouseId, SalesOrderStatus status, LocalDate fromDate, LocalDate toDate);

    List<SupplierPerformanceReportDto> getSupplierPerformanceReport(LocalDate fromDate, LocalDate toDate);

    DashboardSummaryDto getDashboardSummary(UUID warehouseId, LocalDate fromDate, LocalDate toDate);

    List<DashboardWidgetDto> getDashboardWidgets(UUID warehouseId, LocalDate fromDate, LocalDate toDate);

    List<StockAlertDto> getStockAlerts(UUID warehouseId);

    GeneratedReportDto generateReport(GenerateReportRequest request);

    String exportReport(GenerateReportRequest request);

    ReportFileDto exportReportFile(GenerateReportRequest request);

    DataExchangeTemplateDto getImportTemplate(DataExchangeDataset dataset);

    String exportDataset(DataExchangeDataset dataset);
}