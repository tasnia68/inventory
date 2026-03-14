package com.inventory.system.controller;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.common.entity.ReportCategory;
import com.inventory.system.common.entity.ReportOutputFormat;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.AgingAnalysisReportDto;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CurrentStockReportDto;
import com.inventory.system.payload.DataExchangeDataset;
import com.inventory.system.payload.DataExchangeTemplateDto;
import com.inventory.system.payload.DataImportHistoryDto;
import com.inventory.system.payload.DataImportValidationResultDto;
import com.inventory.system.payload.GenerateReportRequest;
import com.inventory.system.payload.GeneratedReportDto;
import com.inventory.system.payload.PurchaseOrderReportDto;
import com.inventory.system.payload.ReportFileDto;
import com.inventory.system.payload.ReportConfigurationDto;
import com.inventory.system.payload.ReportExecutionHistoryDto;
import com.inventory.system.payload.ReportShareDto;
import com.inventory.system.payload.SalesOrderReportDto;
import com.inventory.system.payload.ShareReportRequest;
import com.inventory.system.payload.StockMovementReportDto;
import com.inventory.system.payload.SupplierPerformanceReportDto;
import com.inventory.system.payload.WebhookDeliveryDto;
import com.inventory.system.payload.WebhookEndpointDto;
import com.inventory.system.service.DataExchangeService;
import com.inventory.system.service.ReportSharingService;
import com.inventory.system.service.ReportingService;
import com.inventory.system.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
        private final ReportSharingService reportSharingService;
        private final DataExchangeService dataExchangeService;
        private final WebhookService webhookService;

    @GetMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ReportConfigurationDto>>> getConfigurations(
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getConfigurations(category, active),
                "Report configurations retrieved successfully"));
    }

    @GetMapping("/configurations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ReportConfigurationDto>> getConfiguration(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getConfiguration(id),
                "Report configuration retrieved successfully"));
    }

    @PostMapping("/configurations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportConfigurationDto>> createConfiguration(@Valid @RequestBody ReportConfigurationDto request) {
        return new ResponseEntity<>(ApiResponse.success(
                reportingService.createConfiguration(request),
                "Report configuration created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/configurations/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportConfigurationDto>> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody ReportConfigurationDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.updateConfiguration(id, request),
                "Report configuration updated successfully"));
    }

    @DeleteMapping("/configurations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConfiguration(@PathVariable UUID id) {
        reportingService.deleteConfiguration(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report configuration deleted successfully", null));
    }

    @GetMapping("/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ReportExecutionHistoryDto>>> getExecutions() {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getExecutionHistory(),
                "Report execution history retrieved successfully"));
    }

    @GetMapping("/standard/current-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CurrentStockReportDto>>> getCurrentStock(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productVariantId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getCurrentStockReport(warehouseId, productVariantId),
                "Current stock report retrieved successfully"));
    }

    @GetMapping("/standard/stock-movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<StockMovementReportDto>>> getStockMovements(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productVariantId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getStockMovementReport(warehouseId, productVariantId, fromDate, toDate),
                "Stock movement report retrieved successfully"));
    }

    @GetMapping("/standard/aging-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AgingAnalysisReportDto>>> getAgingAnalysis(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) Integer slowMovingThresholdDays) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getAgingAnalysisReport(warehouseId, slowMovingThresholdDays),
                "Aging analysis report retrieved successfully"));
    }

    @GetMapping("/standard/purchase-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderReportDto>>> getPurchaseOrders(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getPurchaseOrderReport(supplierId, status, fromDate, toDate),
                "Purchase order report retrieved successfully"));
    }

    @GetMapping("/standard/sales-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<SalesOrderReportDto>>> getSalesOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getSalesOrderReport(customerId, warehouseId, status, fromDate, toDate),
                "Sales order report retrieved successfully"));
    }

    @GetMapping("/standard/supplier-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<SupplierPerformanceReportDto>>> getSupplierPerformance(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getSupplierPerformanceReport(fromDate, toDate),
                "Supplier performance report retrieved successfully"));
    }

    @PostMapping("/builder/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<GeneratedReportDto>> executeReport(@RequestBody GenerateReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.generateReport(request),
                "Report generated successfully"));
    }

    @PostMapping("/builder/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
        public ResponseEntity<byte[]> exportReport(@RequestBody GenerateReportRequest request) {
                ReportFileDto file = reportingService.exportReportFile(request);
                MediaType mediaType = MediaType.parseMediaType(file.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                                .body(file.getContent());
    }

        @GetMapping("/configurations/{id}/shares")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
        public ResponseEntity<ApiResponse<List<ReportShareDto>>> getShares(@PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(
                                reportSharingService.getShares(id),
                                "Report shares retrieved successfully"));
        }

        @PostMapping("/configurations/{id}/shares")
        @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
        public ResponseEntity<ApiResponse<ReportShareDto>> shareReport(
                        @PathVariable UUID id,
                        @Valid @RequestBody ShareReportRequest request) {
                return new ResponseEntity<>(ApiResponse.success(
                                reportSharingService.shareReport(id, request),
                                "Report shared successfully"), HttpStatus.CREATED);
        }

        @DeleteMapping("/shares/{shareId}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
        public ResponseEntity<ApiResponse<Void>> revokeShare(@PathVariable UUID shareId) {
                reportSharingService.revokeShare(shareId);
                return ResponseEntity.ok(new ApiResponse<>(true, "Report share revoked successfully", null));
        }

    @GetMapping("/data-exchange/templates/{dataset}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<DataExchangeTemplateDto>> getImportTemplate(@PathVariable DataExchangeDataset dataset) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getImportTemplate(dataset),
                "Import template retrieved successfully"));
    }

    @GetMapping(value = "/data-exchange/export/{dataset}", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<String> exportDataset(@PathVariable DataExchangeDataset dataset) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dataset.name().toLowerCase() + "-export.csv\"")
                .body(reportingService.exportDataset(dataset));
    }

    @PostMapping(value = "/data-exchange/imports/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DataImportValidationResultDto>> validateImport(
            @RequestParam DataExchangeDataset dataset,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                dataExchangeService.validateImport(dataset, file),
                "Import validation completed"));
    }

    @PostMapping(value = "/data-exchange/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DataImportHistoryDto>> startImport(
            @RequestParam DataExchangeDataset dataset,
            @RequestParam("file") MultipartFile file) {
        return new ResponseEntity<>(ApiResponse.success(
                dataExchangeService.startImport(dataset, file),
                "Import started successfully"), HttpStatus.ACCEPTED);
    }

    @GetMapping("/data-exchange/imports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<DataImportHistoryDto>>> getImportHistory() {
        return ResponseEntity.ok(ApiResponse.success(
                dataExchangeService.getImportHistory(),
                "Import history retrieved successfully"));
    }

    @GetMapping("/data-exchange/imports/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<DataImportHistoryDto>> getImportHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                dataExchangeService.getImportHistory(id),
                "Import history item retrieved successfully"));
    }

    @GetMapping("/webhooks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<WebhookEndpointDto>>> getWebhooks() {
        return ResponseEntity.ok(ApiResponse.success(
                webhookService.getEndpoints(),
                "Webhook endpoints retrieved successfully"));
    }

    @PostMapping("/webhooks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<WebhookEndpointDto>> createWebhook(@RequestBody WebhookEndpointDto request) {
        return new ResponseEntity<>(ApiResponse.success(
                webhookService.createEndpoint(request),
                "Webhook endpoint created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/webhooks/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<WebhookEndpointDto>> updateWebhook(
            @PathVariable UUID id,
            @RequestBody WebhookEndpointDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                webhookService.updateEndpoint(id, request),
                "Webhook endpoint updated successfully"));
    }

    @DeleteMapping("/webhooks/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable UUID id) {
        webhookService.deleteEndpoint(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Webhook endpoint deleted successfully", null));
    }

    @GetMapping("/webhooks/deliveries")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryDto>>> getWebhookDeliveries() {
        return ResponseEntity.ok(ApiResponse.success(
                webhookService.getDeliveries(),
                "Webhook delivery history retrieved successfully"));
    }
}