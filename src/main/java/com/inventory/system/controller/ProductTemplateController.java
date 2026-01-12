package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ProductTemplateDto;
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
@RequestMapping("/api/v1/product-templates")
@RequiredArgsConstructor
public class ProductTemplateController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductTemplateDto>> createTemplate(@Valid @RequestBody ProductTemplateDto productTemplateDto) {
        ProductTemplateDto createdTemplate = productService.createTemplate(productTemplateDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product template created successfully", createdTemplate), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductTemplateDto>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody ProductTemplateDto productTemplateDto) {
        ProductTemplateDto updatedTemplate = productService.updateTemplate(id, productTemplateDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product template updated successfully", updatedTemplate), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductTemplateDto>> getTemplate(@PathVariable UUID id) {
        ProductTemplateDto template = productService.getTemplate(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product template retrieved successfully", template), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductTemplateDto>>> getAllTemplates(Pageable pageable) {
        Page<ProductTemplateDto> templates = productService.getAllTemplates(pageable);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product templates retrieved successfully", templates), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        productService.deleteTemplate(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product template deleted successfully", null), HttpStatus.OK);
    }
}
