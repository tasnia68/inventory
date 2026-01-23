package com.inventory.system.controller;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.PurchaseOrderDto;
import com.inventory.system.payload.PurchaseOrderRequest;
import com.inventory.system.payload.PurchaseOrderSearchRequest;
import com.inventory.system.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.createPurchaseOrder(request);
        return new ResponseEntity<>(ApiResponse.success(purchaseOrder, "Purchase Order created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrderById(@PathVariable UUID id) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(purchaseOrder, "Purchase Order retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDto>>> getAllPurchaseOrders(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String poNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        PurchaseOrderSearchRequest searchRequest = new PurchaseOrderSearchRequest();
        searchRequest.setSupplierId(supplierId);
        searchRequest.setStatus(status);
        searchRequest.setPoNumber(poNumber);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);

        Page<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getAllPurchaseOrders(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(purchaseOrders, "Purchase Orders retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> updatePurchaseOrder(@PathVariable UUID id, @Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.updatePurchaseOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(purchaseOrder, "Purchase Order updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> updatePurchaseOrderStatus(@PathVariable UUID id, @RequestParam PurchaseOrderStatus status) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.updatePurchaseOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(purchaseOrder, "Purchase Order status updated successfully"));
    }
}
