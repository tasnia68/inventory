package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ProcurementOverviewDto;
import com.inventory.system.service.ProcurementOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementOverviewService service;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ProcurementOverviewDto>> overview() {
        return ResponseEntity.ok(ApiResponse.success(service.getOverview()));
    }
}
