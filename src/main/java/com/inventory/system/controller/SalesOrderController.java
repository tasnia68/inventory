package com.inventory.system.controller;

import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.ApiResponse;
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
}
