package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> createProduct(@Valid @RequestBody ProductVariantDto productVariantDto) {
        ProductVariantDto createdProduct = productService.createProductVariant(productVariantDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product created successfully", createdProduct), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> getProduct(@PathVariable UUID id) {
        ProductVariantDto product = productService.getProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product retrieved successfully", product), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductVariantDto>>> getAllProducts(Pageable pageable) {
        Page<ProductVariantDto> products = productService.getAllProductVariants(pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Products retrieved successfully", products), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product deleted successfully", null), HttpStatus.OK);
    }
}
