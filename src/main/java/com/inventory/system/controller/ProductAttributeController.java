package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.AttributeGroupDto;
import com.inventory.system.payload.ProductAttributeDto;
import com.inventory.system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductService productService;

    // --- Attribute Group Endpoints ---

    @PostMapping("/attribute-groups")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AttributeGroupDto>> createAttributeGroup(@Valid @RequestBody AttributeGroupDto dto) {
        AttributeGroupDto created = productService.createAttributeGroup(dto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Attribute group created", created), HttpStatus.CREATED);
    }

    @GetMapping("/attribute-groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AttributeGroupDto>>> getAllAttributeGroups() {
        List<AttributeGroupDto> groups = productService.getAllAttributeGroups();
        return new ResponseEntity<>(new ApiResponse<>(true, "Attribute groups retrieved", groups), HttpStatus.OK);
    }

    @GetMapping("/attribute-groups/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<AttributeGroupDto>> getAttributeGroup(@PathVariable UUID id) {
        AttributeGroupDto group = productService.getAttributeGroup(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Attribute group retrieved", group), HttpStatus.OK);
    }

    @DeleteMapping("/attribute-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAttributeGroup(@PathVariable UUID id) {
        productService.deleteAttributeGroup(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Attribute group deleted", null), HttpStatus.OK);
    }

    // --- Product Attribute Endpoints ---

    @PostMapping("/product-attributes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductAttributeDto>> createProductAttribute(@Valid @RequestBody ProductAttributeDto dto) {
        ProductAttributeDto created = productService.createProductAttribute(dto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product attribute created", created), HttpStatus.CREATED);
    }

    @PutMapping("/product-attributes/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductAttributeDto>> updateProductAttribute(@PathVariable UUID id, @Valid @RequestBody ProductAttributeDto dto) {
        ProductAttributeDto updated = productService.updateProductAttribute(id, dto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product attribute updated", updated), HttpStatus.OK);
    }

    @GetMapping("/product-attributes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductAttributeDto>> getProductAttribute(@PathVariable UUID id) {
        ProductAttributeDto attribute = productService.getProductAttribute(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product attribute retrieved", attribute), HttpStatus.OK);
    }

    @GetMapping("/product-attributes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductAttributeDto>>> getAttributesByTemplate(@RequestParam(required = false) UUID templateId) {
        if (templateId != null) {
            List<ProductAttributeDto> attributes = productService.getAttributesByTemplate(templateId);
            return new ResponseEntity<>(new ApiResponse<>(true, "Product attributes retrieved", attributes), HttpStatus.OK);
        } else {
             return new ResponseEntity<>(new ApiResponse<>(false, "Please provide templateId", null), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/product-attributes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProductAttribute(@PathVariable UUID id) {
        productService.deleteProductAttribute(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Product attribute deleted", null), HttpStatus.OK);
    }
}
