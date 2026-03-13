package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.BulkProductOperationRequest;
import com.inventory.system.payload.BulkProductOperationResultDto;
import com.inventory.system.payload.ProductImportResultDto;
import com.inventory.system.payload.ProductSearchRequest;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.payload.ProductVariantVersionDto;
import com.inventory.system.payload.SimpleProductDto;
import com.inventory.system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable) {
        if (categoryId != null) {
            Page<ProductVariantDto> byCategory = productService.getProductVariantsByCategory(categoryId, pageable);
            return new ResponseEntity<>(new ApiResponse<>(true, "Products retrieved successfully", byCategory), HttpStatus.OK);
        }
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
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) UUID attributeId,
            @RequestParam(required = false) String attributeValue,
            Pageable pageable) {
        if ((q == null || q.isBlank()) && categoryId == null && templateId == null && attributeId == null
                && (attributeValue == null || attributeValue.isBlank())) {
            return new ResponseEntity<>(new ApiResponse<>(false, "At least one search or filter parameter is required", null), HttpStatus.BAD_REQUEST);
        }
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQ(q);
        request.setCategoryId(categoryId);
        request.setTemplateId(templateId);
        request.setAttributeId(attributeId);
        request.setAttributeValue(attributeValue);
        Page<ProductVariantDto> results = productService.searchProductVariants(request, pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Products retrieved successfully", results), HttpStatus.OK);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BulkProductOperationResultDto>> bulkOperation(
            @RequestBody BulkProductOperationRequest request) {
        BulkProductOperationResultDto result = productService.bulkOperateProducts(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Bulk product operation completed", result), HttpStatus.OK);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductImportResultDto>> importProducts(@RequestParam("file") MultipartFile file) {
        ProductImportResultDto result = productService.importProductsFromCsv(file);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product import completed", result), HttpStatus.OK);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<String> exportProducts() {
        return ResponseEntity.ok(productService.exportProductsToCsv());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductVariantVersionDto>>> getProductHistory(
            @PathVariable UUID id,
            Pageable pageable) {
        Page<ProductVariantVersionDto> history = productService.getProductVariantHistory(id, pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product history retrieved successfully", history), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProductVariant(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product deleted successfully", null), HttpStatus.OK);
    }
}
