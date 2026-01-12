package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.payload.AttributeGroupDto;
import com.inventory.system.payload.ProductAttributeDto;
import com.inventory.system.payload.ProductTemplateDto;
import com.inventory.system.payload.ProductVariantDto;
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
    ProductVariantDto getProductVariant(UUID id);
    Page<ProductVariantDto> getAllProductVariants(Pageable pageable);
    void deleteProductVariant(UUID id);
}
