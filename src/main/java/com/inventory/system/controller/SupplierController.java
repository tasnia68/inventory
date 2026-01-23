package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateSupplierRequest;
import com.inventory.system.payload.SupplierDto;
import com.inventory.system.payload.UpdateSupplierRequest;
import com.inventory.system.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierDto supplier = supplierService.createSupplier(request);
        ApiResponse<SupplierDto> response = new ApiResponse<>(true, "Supplier created successfully", supplier);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> getSupplierById(@PathVariable UUID id) {
        SupplierDto supplier = supplierService.getSupplierById(id);
        ApiResponse<SupplierDto> response = new ApiResponse<>(true, "Supplier retrieved successfully", supplier);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierDto>>> getAllSuppliers() {
        List<SupplierDto> suppliers = supplierService.getAllSuppliers();
        ApiResponse<List<SupplierDto>> response = new ApiResponse<>(true, "Suppliers retrieved successfully", suppliers);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> updateSupplier(@PathVariable UUID id, @RequestBody UpdateSupplierRequest request) {
        SupplierDto supplier = supplierService.updateSupplier(id, request);
        ApiResponse<SupplierDto> response = new ApiResponse<>(true, "Supplier updated successfully", supplier);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        ApiResponse<Void> response = new ApiResponse<>(true, "Supplier deleted successfully", null);
        return ResponseEntity.ok(response);
    }
}
