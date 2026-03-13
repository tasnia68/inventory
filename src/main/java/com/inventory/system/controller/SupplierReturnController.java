package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateSupplierReturnRequest;
import com.inventory.system.payload.SupplierReturnDto;
import com.inventory.system.service.SupplierReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SupplierReturnController {

    private final SupplierReturnService supplierReturnService;

    @PostMapping("/supplier-returns")
    public ResponseEntity<ApiResponse<SupplierReturnDto>> createSupplierReturn(@Valid @RequestBody CreateSupplierReturnRequest request) {
        SupplierReturnDto supplierReturn = supplierReturnService.createSupplierReturn(request);
        return new ResponseEntity<>(ApiResponse.success(supplierReturn, "Supplier return created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/supplier-returns/{id}")
    public ResponseEntity<ApiResponse<SupplierReturnDto>> getSupplierReturn(@PathVariable UUID id) {
        SupplierReturnDto supplierReturn = supplierReturnService.getSupplierReturn(id);
        return ResponseEntity.ok(ApiResponse.success(supplierReturn, "Supplier return retrieved successfully"));
    }

    @GetMapping("/goods-receipt-notes/{goodsReceiptNoteId}/supplier-returns")
    public ResponseEntity<ApiResponse<List<SupplierReturnDto>>> getReturnsForGoodsReceipt(@PathVariable UUID goodsReceiptNoteId) {
        List<SupplierReturnDto> supplierReturns = supplierReturnService.getReturnsForGoodsReceipt(goodsReceiptNoteId);
        return ResponseEntity.ok(ApiResponse.success(supplierReturns, "Supplier returns retrieved successfully"));
    }

    @PostMapping("/supplier-returns/{id}/confirm")
    public ResponseEntity<ApiResponse<SupplierReturnDto>> confirmSupplierReturn(@PathVariable UUID id) {
        SupplierReturnDto supplierReturn = supplierReturnService.confirmSupplierReturn(id);
        return ResponseEntity.ok(ApiResponse.success(supplierReturn, "Supplier return confirmed successfully"));
    }

    @PostMapping("/supplier-returns/{id}/cancel")
    public ResponseEntity<ApiResponse<SupplierReturnDto>> cancelSupplierReturn(@PathVariable UUID id) {
        SupplierReturnDto supplierReturn = supplierReturnService.cancelSupplierReturn(id);
        return ResponseEntity.ok(ApiResponse.success(supplierReturn, "Supplier return cancelled successfully"));
    }
}