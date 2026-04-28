package com.inventory.system.controller;

import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ConfirmOrderRequest;
import com.inventory.system.payload.HoldOrderRequest;
import com.inventory.system.payload.PartialDeliveryLineRequest;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.SalesOrderSearchRequest;
import com.inventory.system.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<SalesOrderDto>> createSalesOrder(@Valid @RequestBody SalesOrderRequest request) {
        SalesOrderDto salesOrder = salesOrderService.createSalesOrder(request);
        return new ResponseEntity<>(ApiResponse.success(salesOrder, "Sales Order created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getSalesOrderById(@PathVariable UUID id) {
        SalesOrderDto salesOrder = salesOrderService.getSalesOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(salesOrder, "Sales Order retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SalesOrderDto>>> getAllSalesOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) String soNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        SalesOrderSearchRequest searchRequest = new SalesOrderSearchRequest();
        searchRequest.setCustomerId(customerId);
        searchRequest.setWarehouseId(warehouseId);
        searchRequest.setStatus(status);
        searchRequest.setPriority(priority);
        searchRequest.setSoNumber(soNumber);
        searchRequest.setStartDate(startDate);
        searchRequest.setEndDate(endDate);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);

        Page<SalesOrderDto> salesOrders = salesOrderService.getAllSalesOrders(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(salesOrders, "Sales Orders retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateSalesOrder(@PathVariable UUID id, @Valid @RequestBody SalesOrderRequest request) {
        SalesOrderDto salesOrder = salesOrderService.updateSalesOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(salesOrder, "Sales Order updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateSalesOrderStatus(@PathVariable UUID id, @RequestParam SalesOrderStatus status) {
        SalesOrderDto salesOrder = salesOrderService.updateSalesOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(salesOrder, "Sales Order status updated successfully"));
    }

    @GetMapping("/{id}/allowed-transitions")
    public ResponseEntity<ApiResponse<java.util.Set<SalesOrderStatus>>> allowedTransitions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.getAllowedTransitions(id), "OK"));
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<ApiResponse<SalesOrderDto>> hold(@PathVariable UUID id, @RequestBody(required = false) HoldOrderRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.holdOrder(id, reason), "Order placed on hold"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<SalesOrderDto>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.APPROVED), "Order approved"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<SalesOrderDto>> confirm(@PathVariable UUID id, @RequestBody(required = false) ConfirmOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.confirmOrder(id, request), "Order confirmed"));
    }

    @PostMapping("/{id}/pack-complete")
    public ResponseEntity<ApiResponse<SalesOrderDto>> packComplete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.PACKAGING), "Order packaged"));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<SalesOrderDto>> ship(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.SHIPPED), "Order shipped"));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<SalesOrderDto>> deliver(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.DELIVERED), "Order delivered"));
    }

    @PostMapping("/{id}/partial-deliver")
    public ResponseEntity<ApiResponse<SalesOrderDto>> partialDeliver(@PathVariable UUID id, @Valid @RequestBody java.util.List<PartialDeliveryLineRequest> lines) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.partialDeliver(id, lines), "Partial delivery recorded"));
    }

    @PatchMapping("/{id}/items")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateItems(@PathVariable UUID id, @Valid @RequestBody java.util.List<com.inventory.system.payload.SalesOrderItemRequest> items) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateItems(id, items), "Items updated"));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<SalesOrderDto>> markReturned(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.RETURNED), "Order returned"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalesOrderDto>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salesOrderService.updateSalesOrderStatus(id, SalesOrderStatus.CANCELLED), "Order cancelled"));
    }
}
