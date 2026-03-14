package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateSupplierClaimRequest;
import com.inventory.system.payload.SupplierClaimDto;
import com.inventory.system.service.SupplierClaimService;
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
public class SupplierClaimController {

    private final SupplierClaimService supplierClaimService;

    @PostMapping("/goods-receipt-notes/{goodsReceiptNoteId}/supplier-claims")
    public ResponseEntity<ApiResponse<SupplierClaimDto>> createSupplierClaim(
            @PathVariable UUID goodsReceiptNoteId,
            @Valid @RequestBody CreateSupplierClaimRequest request) {
        SupplierClaimDto claim = supplierClaimService.createSupplierClaim(goodsReceiptNoteId, request);
        return new ResponseEntity<>(ApiResponse.success(claim, "Supplier claim created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/goods-receipt-notes/{goodsReceiptNoteId}/supplier-claims")
    public ResponseEntity<ApiResponse<List<SupplierClaimDto>>> getClaimsForGoodsReceipt(@PathVariable UUID goodsReceiptNoteId) {
        List<SupplierClaimDto> claims = supplierClaimService.getClaimsForGoodsReceipt(goodsReceiptNoteId);
        return ResponseEntity.ok(ApiResponse.success(claims, "Supplier claims retrieved successfully"));
    }

    @GetMapping("/supplier-claims/{id}")
    public ResponseEntity<ApiResponse<SupplierClaimDto>> getSupplierClaim(@PathVariable UUID id) {
        SupplierClaimDto claim = supplierClaimService.getSupplierClaim(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Supplier claim retrieved successfully"));
    }

    @PostMapping("/supplier-claims/{id}/supplier-return")
    public ResponseEntity<ApiResponse<SupplierClaimDto>> createSupplierReturnFromClaim(@PathVariable UUID id) {
        SupplierClaimDto claim = supplierClaimService.createSupplierReturnFromClaim(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Supplier return created from claim successfully"));
    }
}