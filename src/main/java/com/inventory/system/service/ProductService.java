package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.payload.AttributeGroupDto;
import com.inventory.system.payload.BulkProductOperationRequest;
import com.inventory.system.payload.BulkProductOperationResultDto;
import com.inventory.system.payload.ProductImportResultDto;
import com.inventory.system.payload.ProductAttributeDto;
import com.inventory.system.payload.ProductSearchRequest;
import com.inventory.system.payload.ProductTemplateDto;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.payload.ProductVariantVersionDto;
import com.inventory.system.payload.SimpleProductDto;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    String generateSku(ProductVariant variant);

    // Template
    ProductTemplateDto createTemplate(ProductTemplateDto productTemplateDto);
    ProductTemplateDto updateTemplate(UUID id, ProductTemplateDto productTemplateDto);
    ProductTemplateDto getTemplate(UUID id);
    Page<ProductTemplateDto> getAllTemplates(Pageable pageable);
    void deleteTemplate(UUID id);

    // Attribute Group
    AttributeGroupDto createAttributeGroup(AttributeGroupDto attributeGroupDto);
    AttributeGroupDto getAttributeGroup(UUID id);
    List<AttributeGroupDto> getAllAttributeGroups();
    void deleteAttributeGroup(UUID id);

    // Attribute
    ProductAttributeDto createProductAttribute(ProductAttributeDto productAttributeDto);
    ProductAttributeDto updateProductAttribute(UUID id, ProductAttributeDto productAttributeDto);
    ProductAttributeDto getProductAttribute(UUID id);
    List<ProductAttributeDto> getAttributesByTemplate(UUID templateId);
    void deleteProductAttribute(UUID id);

    // Variant
    ProductVariantDto createProductVariant(ProductVariantDto productVariantDto);
    ProductVariantDto updateProductVariant(UUID id, ProductVariantDto productVariantDto);
    ProductVariantDto getProductVariant(UUID id);
    Page<ProductVariantDto> getAllProductVariants(Pageable pageable);
    Page<ProductVariantDto> getProductVariantsByCategory(UUID categoryId, Pageable pageable);
    List<ProductVariantDto> getProductVariantsByTemplate(UUID templateId);
    Page<ProductVariantDto> searchProductVariants(String query, Pageable pageable);
    Page<ProductVariantDto> searchProductVariants(ProductSearchRequest request, Pageable pageable);
    void deleteProductVariant(UUID id);

    BulkProductOperationResultDto bulkOperateProducts(BulkProductOperationRequest request);
    ProductImportResultDto importProductsFromCsv(MultipartFile file);
    String exportProductsToCsv();

    Page<ProductVariantVersionDto> getProductVariantHistory(UUID variantId, Pageable pageable);

    // Simple Product (Convenience API)
    ProductVariantDto createSimpleProduct(SimpleProductDto simpleProductDto);
}
