package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.payload.SimpleProductDto;
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

    @PostMapping("/simple")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> createSimpleProduct(@Valid @RequestBody SimpleProductDto simpleProductDto) {
        ProductVariantDto createdProduct = productService.createSimpleProduct(simpleProductDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Simple product created successfully", createdProduct), HttpStatus.CREATED);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> createProduct(@Valid @RequestBody ProductVariantDto productVariantDto) {
        ProductVariantDto createdProduct = productService.createProductVariant(productVariantDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product created successfully", createdProduct), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantDto productVariantDto) {
        ProductVariantDto updatedProduct = productService.updateProductVariant(id, productVariantDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product updated successfully", updatedProduct), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> getProduct(@PathVariable UUID id) {
        ProductVariantDto product = productService.getProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product retrieved successfully", product), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<?>> getAllProducts(
            @RequestParam(required = false) UUID templateId,
            Pageable pageable) {
        if (templateId != null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(true, "Products retrieved successfully",
                            productService.getProductVariantsByTemplate(templateId)),
                    HttpStatus.OK);
        }
        Page<ProductVariantDto> products = productService.getAllProductVariants(pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Products retrieved successfully", products), HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductVariantDto>>> searchProducts(
            @RequestParam("q") String query,
            Pageable pageable) {
        if (query == null || query.isBlank()) {
            return new ResponseEntity<>(new ApiResponse<>(false, "Query is required", null), HttpStatus.BAD_REQUEST);
        }
        Page<ProductVariantDto> results = productService.searchProductVariants(query, pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Products retrieved successfully", results), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product deleted successfully", null), HttpStatus.OK);
    }
}
