package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateSupplierProductRequest;
import com.inventory.system.payload.SupplierProductDto;
import com.inventory.system.payload.UpdateSupplierProductRequest;
import com.inventory.system.service.SupplierProductService;
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
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    @PostMapping("/{supplierId}/products")
    public ResponseEntity<ApiResponse<SupplierProductDto>> addProductToSupplier(
            @PathVariable UUID supplierId,
            @Valid @RequestBody CreateSupplierProductRequest request) {
        // Ensure the path variable supplierId matches the request body if needed,
        // or just override/validate it. Here we trust the request body but it's good practice to align.
        if (!supplierId.equals(request.getSupplierId())) {
            request.setSupplierId(supplierId);
        }
        SupplierProductDto dto = supplierProductService.createSupplierProduct(request);
        ApiResponse<SupplierProductDto> response = new ApiResponse<>(true, "Product added to supplier successfully", dto);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{supplierId}/products")
    public ResponseEntity<ApiResponse<List<SupplierProductDto>>> getProductsBySupplier(@PathVariable UUID supplierId) {
        List<SupplierProductDto> list = supplierProductService.getProductsBySupplier(supplierId);
        ApiResponse<List<SupplierProductDto>> response = new ApiResponse<>(true, "Supplier products retrieved successfully", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/variant/{productVariantId}")
    public ResponseEntity<ApiResponse<List<SupplierProductDto>>> getSuppliersByProduct(@PathVariable UUID productVariantId) {
        List<SupplierProductDto> list = supplierProductService.getSuppliersByProduct(productVariantId);
        ApiResponse<List<SupplierProductDto>> response = new ApiResponse<>(true, "Product suppliers retrieved successfully", list);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<SupplierProductDto>> updateSupplierProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierProductRequest request) {
        SupplierProductDto dto = supplierProductService.updateSupplierProduct(id, request);
        ApiResponse<SupplierProductDto> response = new ApiResponse<>(true, "Supplier product updated successfully", dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> removeProductFromSupplier(@PathVariable UUID id) {
        supplierProductService.deleteSupplierProduct(id);
        ApiResponse<Void> response = new ApiResponse<>(true, "Product removed from supplier successfully", null);
        return ResponseEntity.ok(response);
    }
}
