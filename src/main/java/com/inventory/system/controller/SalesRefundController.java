package com.inventory.system.controller;

import com.inventory.system.common.entity.SalesRefundStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateSalesRefundRequest;
import com.inventory.system.payload.RefundDocumentDto;
import com.inventory.system.payload.RefundStatusDecisionRequest;
import com.inventory.system.payload.SalesRefundDto;
import com.inventory.system.service.SalesRefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-refunds")
@RequiredArgsConstructor
public class SalesRefundController {

    private final SalesRefundService salesRefundService;

    @PostMapping
    public ResponseEntity<ApiResponse<SalesRefundDto>> createRefund(@Valid @RequestBody CreateSalesRefundRequest request) {
        SalesRefundDto refund = salesRefundService.createRefund(request);
        return new ResponseEntity<>(ApiResponse.success(refund, "Sales refund created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesRefundDto>> getRefund(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.getRefund(id), "Sales refund retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SalesRefundDto>>> getRefunds(
            @RequestParam(required = false) UUID salesOrderId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) SalesRefundStatus status,
            @RequestParam(required = false) String refundNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Page<SalesRefundDto> data = salesRefundService.getRefunds(salesOrderId, customerId, status, refundNumber, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(data, "Sales refunds retrieved successfully"));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<SalesRefundDto>> approveRefund(@PathVariable UUID id,
                                                                     @RequestBody(required = false) RefundStatusDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.approveRefund(id, request), "Sales refund approved successfully"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<SalesRefundDto>> rejectRefund(@PathVariable UUID id,
                                                                    @RequestBody(required = false) RefundStatusDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.rejectRefund(id, request), "Sales refund rejected successfully"));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<SalesRefundDto>> completeRefund(@PathVariable UUID id,
                                                                      @RequestBody(required = false) RefundStatusDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.completeRefund(id, request), "Sales refund completed successfully"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalesRefundDto>> cancelRefund(@PathVariable UUID id,
                                                                    @RequestBody(required = false) RefundStatusDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.cancelRefund(id, request), "Sales refund cancelled successfully"));
    }

    @PostMapping("/{id}/credit-note")
    public ResponseEntity<ApiResponse<RefundDocumentDto>> generateCreditNote(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesRefundService.generateCreditNote(id), "Credit note generated successfully"));
    }
}