package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.DashboardSummaryDto;
import com.inventory.system.payload.DashboardWidgetDto;
import com.inventory.system.payload.StockAlertDto;
import com.inventory.system.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboard(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getDashboardSummary(warehouseId, fromDate, toDate),
                "Dashboard summary retrieved successfully"));
    }

    @GetMapping("/widgets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<DashboardWidgetDto>>> getWidgets(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getDashboardWidgets(warehouseId, fromDate, toDate),
                "Dashboard widgets retrieved successfully"));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<StockAlertDto>>> getAlerts(@RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getStockAlerts(warehouseId),
                "Stock alerts retrieved successfully"));
    }
}