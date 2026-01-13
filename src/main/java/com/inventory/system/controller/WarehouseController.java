package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateWarehouseRequest;
import com.inventory.system.payload.UpdateWarehouseRequest;
import com.inventory.system.payload.WarehouseDto;
import com.inventory.system.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseDto warehouse = warehouseService.createWarehouse(request);
        ApiResponse<WarehouseDto> response = new ApiResponse<>(true, "Warehouse created successfully", warehouse);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouseById(@PathVariable UUID id) {
        WarehouseDto warehouse = warehouseService.getWarehouseById(id);
        ApiResponse<WarehouseDto> response = new ApiResponse<>(true, "Warehouse retrieved successfully", warehouse);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseDto>>> getAllWarehouses() {
        List<WarehouseDto> warehouses = warehouseService.getAllWarehouses();
        ApiResponse<List<WarehouseDto>> response = new ApiResponse<>(true, "Warehouses retrieved successfully", warehouses);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDto>> updateWarehouse(@PathVariable UUID id, @RequestBody UpdateWarehouseRequest request) {
        WarehouseDto warehouse = warehouseService.updateWarehouse(id, request);
        ApiResponse<WarehouseDto> response = new ApiResponse<>(true, "Warehouse updated successfully", warehouse);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable UUID id) {
        warehouseService.deleteWarehouse(id);
        ApiResponse<Void> response = new ApiResponse<>(true, "Warehouse deleted successfully", null);
        return ResponseEntity.ok(response);
    }
}
