package com.inventory.system.controller;

import com.inventory.system.common.entity.FinancialEventType;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.FinancialEventDto;
import com.inventory.system.service.FinancialEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial-events")
@RequiredArgsConstructor
public class FinancialEventController {

    private final FinancialEventService financialEventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FinancialEventDto>>> getFinancialEvents(
            @RequestParam(required = false) PostingStatus postingStatus,
            @RequestParam(required = false) FinancialEventType eventType,
            @RequestParam(required = false) String sourceDocumentType) {
        return ResponseEntity.ok(ApiResponse.success(
                financialEventService.getFinancialEvents(postingStatus, eventType, sourceDocumentType)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FinancialEventDto>> getFinancialEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(financialEventService.getFinancialEvent(id)));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<FinancialEventDto>> retryFinancialEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                financialEventService.retryFinancialEvent(id),
                "Financial event moved back to pending"
        ));
    }
}
