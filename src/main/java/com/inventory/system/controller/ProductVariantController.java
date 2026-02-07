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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> createVariant(@Valid @RequestBody ProductVariantDto productVariantDto) {
        ProductVariantDto created = productService.createProductVariant(productVariantDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product variant created", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> updateVariant(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantDto productVariantDto) {
        ProductVariantDto updated = productService.updateProductVariant(id, productVariantDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product variant updated", updated), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> getVariant(@PathVariable UUID id) {
        ProductVariantDto product = productService.getProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product variant retrieved", product), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<?>> getVariants(
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        if (q != null && !q.isBlank()) {
            Page<ProductVariantDto> results = productService.searchProductVariants(q, pageable);
            return new ResponseEntity<>(new ApiResponse<>(true, "Product variants retrieved", results), HttpStatus.OK);
        }
        if (templateId != null) {
            List<ProductVariantDto> list = productService.getProductVariantsByTemplate(templateId);
            return new ResponseEntity<>(new ApiResponse<>(true, "Product variants retrieved", list), HttpStatus.OK);
        }
        Page<ProductVariantDto> page = productService.getAllProductVariants(pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product variants retrieved", page), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable UUID id) {
        productService.deleteProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product variant deleted", null), HttpStatus.OK);
    }
}
