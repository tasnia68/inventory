package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.InventoryValuationReportDto;
import com.inventory.system.service.InventoryValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/valuation")
@RequiredArgsConstructor
public class InventoryValuationController {

    private final InventoryValuationService valuationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryValuationReportDto>>> getValuationReport(
            @RequestParam(required = false) UUID warehouseId) {
        List<InventoryValuationReportDto> report = valuationService.getValuationReport(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
