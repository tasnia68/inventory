package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AttributeGroupDto;
import com.inventory.system.payload.ProductAttributeDto;
import com.inventory.system.payload.ProductTemplateDto;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.repository.AttributeGroupRepository;
import com.inventory.system.repository.ProductAttributeRepository;
import com.inventory.system.repository.ProductAttributeValueRepository;
import com.inventory.system.repository.ProductTemplateRepository;
import com.inventory.system.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductTemplateRepository productTemplateRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final AttributeGroupRepository attributeGroupRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    public String generateSku(ProductVariant variant) {
        // Simple SKU generation logic: TEMPLATE_NAME-RANDOM_UUID_SUBSTRING
        // In a real scenario, this would be more complex, possibly involving attributes.
        String templateName = variant.getTemplate() != null ? variant.getTemplate().getName() : "UNK";
        String prefix = templateName.length() > 3 ? templateName.substring(0, 3).toUpperCase() : templateName.toUpperCase();
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // --- Template Methods ---

    @Override
    @Transactional
    public ProductTemplateDto createTemplate(ProductTemplateDto productTemplateDto) {
        ProductTemplate template = new ProductTemplate();
        template.setName(productTemplateDto.getName());
        template.setDescription(productTemplateDto.getDescription());
        template.setIsActive(productTemplateDto.getIsActive() != null ? productTemplateDto.getIsActive() : true);

        template = productTemplateRepository.save(template);
        return mapToDto(template);
    }

    @Override
    @Transactional
    public ProductTemplateDto updateTemplate(UUID id, ProductTemplateDto productTemplateDto) {
        ProductTemplate template = productTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", id));

        template.setName(productTemplateDto.getName());
        template.setDescription(productTemplateDto.getDescription());
        if (productTemplateDto.getIsActive() != null) {
            template.setIsActive(productTemplateDto.getIsActive());
        }

        template = productTemplateRepository.save(template);
        return mapToDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTemplateDto getTemplate(UUID id) {
        ProductTemplate template = productTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", id));
        return mapToDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductTemplateDto> getAllTemplates(Pageable pageable) {
        return productTemplateRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        ProductTemplate template = productTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", id));
        productTemplateRepository.delete(template);
    }

    // --- Attribute Group Methods ---

    @Override
    @Transactional
    public AttributeGroupDto createAttributeGroup(AttributeGroupDto attributeGroupDto) {
        AttributeGroup group = new AttributeGroup();
        group.setName(attributeGroupDto.getName());
        group.setDescription(attributeGroupDto.getDescription());

        group = attributeGroupRepository.save(group);
        return mapToDto(group);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeGroupDto getAttributeGroup(UUID id) {
        AttributeGroup group = attributeGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute Group", "id", id));
        return mapToDto(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeGroupDto> getAllAttributeGroups() {
        return attributeGroupRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAttributeGroup(UUID id) {
        AttributeGroup group = attributeGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute Group", "id", id));
        attributeGroupRepository.delete(group);
    }

    // --- Attribute Methods ---

    @Override
    @Transactional
    public ProductAttributeDto createProductAttribute(ProductAttributeDto dto) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName(dto.getName());
        attribute.setType(dto.getType());
        attribute.setRequired(dto.getRequired() != null ? dto.getRequired() : false);
        attribute.setValidationRegex(dto.getValidationRegex());
        attribute.setOptions(dto.getOptions());

        if (dto.getTemplateId() != null) {
            ProductTemplate template = productTemplateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", dto.getTemplateId()));
            attribute.setTemplate(template);
        }

        if (dto.getGroupId() != null) {
            AttributeGroup group = attributeGroupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attribute Group", "id", dto.getGroupId()));
            attribute.setGroup(group);
        }

        attribute = productAttributeRepository.save(attribute);
        return mapToDto(attribute);
    }

    @Override
    @Transactional
    public ProductAttributeDto updateProductAttribute(UUID id, ProductAttributeDto dto) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id", id));

        attribute.setName(dto.getName());
        if(dto.getType() != null) attribute.setType(dto.getType());
        if(dto.getRequired() != null) attribute.setRequired(dto.getRequired());
        attribute.setValidationRegex(dto.getValidationRegex());
        attribute.setOptions(dto.getOptions());

         if (dto.getTemplateId() != null) {
            ProductTemplate template = productTemplateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", dto.getTemplateId()));
            attribute.setTemplate(template);
        }

        if (dto.getGroupId() != null) {
            AttributeGroup group = attributeGroupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attribute Group", "id", dto.getGroupId()));
            attribute.setGroup(group);
        }

        attribute = productAttributeRepository.save(attribute);
        return mapToDto(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeDto getProductAttribute(UUID id) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id", id));
        return mapToDto(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeDto> getAttributesByTemplate(UUID templateId) {
        return productAttributeRepository.findByTemplateId(templateId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProductAttribute(UUID id) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id", id));
        productAttributeRepository.delete(attribute);
    }

    // --- Variant Methods ---

    @Override
    @Transactional
    public ProductVariantDto createProductVariant(ProductVariantDto dto) {
        ProductTemplate template = productTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", dto.getTemplateId()));

        ProductVariant variant = new ProductVariant();
        variant.setTemplate(template);
        variant.setPrice(dto.getPrice());

        // Generate SKU if not provided, or check uniqueness if provided
        if (dto.getSku() == null || dto.getSku().isEmpty()) {
            variant.setSku(generateSku(variant));
        } else {
             if (productVariantRepository.existsBySku(dto.getSku())) {
                throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
             }
             variant.setSku(dto.getSku());
        }

        variant = productVariantRepository.save(variant);

        // Handle attributes
        if (dto.getAttributeValues() != null) {
            List<ProductAttributeValue> attributeValues = new ArrayList<>();
            for (ProductVariantDto.AttributeValueDto valDto : dto.getAttributeValues()) {
                ProductAttribute attribute = productAttributeRepository.findById(valDto.getAttributeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id", valDto.getAttributeId()));

                validateAttributeValue(attribute, valDto.getValue());

                ProductAttributeValue attributeValue = new ProductAttributeValue();
                attributeValue.setVariant(variant);
                attributeValue.setAttribute(attribute);
                attributeValue.setValue(valDto.getValue());

                attributeValues.add(attributeValue);
            }

            // Validate required attributes
            List<ProductAttribute> requiredAttributes = productAttributeRepository.findByTemplateId(template.getId())
                .stream().filter(ProductAttribute::getRequired).collect(Collectors.toList());

            List<UUID> providedAttributeIds = dto.getAttributeValues().stream()
                .map(ProductVariantDto.AttributeValueDto::getAttributeId).collect(Collectors.toList());

            for (ProductAttribute required : requiredAttributes) {
                if (!providedAttributeIds.contains(required.getId())) {
                    throw new IllegalArgumentException("Missing required attribute: " + required.getName());
                }
            }

            productAttributeValueRepository.saveAll(attributeValues);
            variant.setAttributeValues(attributeValues);
        } else {
             // If no attributes provided, check if there are any required ones
             List<ProductAttribute> requiredAttributes = productAttributeRepository.findByTemplateId(template.getId())
                .stream().filter(ProductAttribute::getRequired).collect(Collectors.toList());
             if (!requiredAttributes.isEmpty()) {
                 throw new IllegalArgumentException("Missing required attributes");
             }
        }

        return mapToDto(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantDto getProductVariant(UUID id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant", "id", id));
        return mapToDto(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantDto> getAllProductVariants(Pageable pageable) {
        return productVariantRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional
    public void deleteProductVariant(UUID id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant", "id", id));
        productVariantRepository.delete(variant);
    }

    // --- Mappers ---

    private ProductTemplateDto mapToDto(ProductTemplate template) {
        ProductTemplateDto dto = new ProductTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setUpdatedBy(template.getUpdatedBy());
        return dto;
    }

    private AttributeGroupDto mapToDto(AttributeGroup group) {
        AttributeGroupDto dto = new AttributeGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setUpdatedBy(group.getUpdatedBy());
        return dto;
    }

    private ProductAttributeDto mapToDto(ProductAttribute attribute) {
        ProductAttributeDto dto = new ProductAttributeDto();
        dto.setId(attribute.getId());
        dto.setName(attribute.getName());
        dto.setType(attribute.getType());
        dto.setRequired(attribute.getRequired());
        dto.setValidationRegex(attribute.getValidationRegex());
        dto.setOptions(attribute.getOptions());
        if(attribute.getTemplate() != null) dto.setTemplateId(attribute.getTemplate().getId());
        if(attribute.getGroup() != null) dto.setGroupId(attribute.getGroup().getId());
        dto.setCreatedAt(attribute.getCreatedAt());
        dto.setUpdatedAt(attribute.getUpdatedAt());
        dto.setCreatedBy(attribute.getCreatedBy());
        dto.setUpdatedBy(attribute.getUpdatedBy());
        return dto;
    }

    private ProductVariantDto mapToDto(ProductVariant variant) {
        ProductVariantDto dto = new ProductVariantDto();
        dto.setId(variant.getId());
        dto.setSku(variant.getSku());
        dto.setPrice(variant.getPrice());
        if (variant.getTemplate() != null) dto.setTemplateId(variant.getTemplate().getId());
        dto.setCreatedAt(variant.getCreatedAt());
        dto.setUpdatedAt(variant.getUpdatedAt());
        dto.setCreatedBy(variant.getCreatedBy());
        dto.setUpdatedBy(variant.getUpdatedBy());

        // Map attribute values
        if (variant.getAttributeValues() != null) {
            dto.setAttributeValues(variant.getAttributeValues().stream().map(val -> {
                ProductVariantDto.AttributeValueDto valDto = new ProductVariantDto.AttributeValueDto();
                valDto.setAttributeId(val.getAttribute().getId());
                valDto.setValue(val.getValue());
                return valDto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }

    private void validateAttributeValue(ProductAttribute attribute, String value) {
        if (value == null && attribute.getRequired()) {
            throw new IllegalArgumentException("Attribute " + attribute.getName() + " is required.");
        }

        if (value != null) {
            // Regex validation
            if (attribute.getValidationRegex() != null && !attribute.getValidationRegex().isEmpty()) {
                if (!value.matches(attribute.getValidationRegex())) {
                    throw new IllegalArgumentException("Value '" + value + "' for attribute " + attribute.getName() + " does not match pattern: " + attribute.getValidationRegex());
                }
            }

            // Options validation (DROPDOWN/MULTI_SELECT)
            if ((attribute.getType() == ProductAttribute.AttributeType.DROPDOWN || attribute.getType() == ProductAttribute.AttributeType.MULTI_SELECT) && attribute.getOptions() != null) {
                // Assuming options are comma-separated or JSON list. For simplicity here: comma-separated.
                // In production, robust parsing is needed.
                List<String> allowedOptions = Arrays.stream(attribute.getOptions().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());

                if (attribute.getType() == ProductAttribute.AttributeType.DROPDOWN) {
                    if (!allowedOptions.contains(value)) {
                         throw new IllegalArgumentException("Value '" + value + "' is not a valid option for " + attribute.getName());
                    }
                } else {
                    // MULTI_SELECT: value might be comma separated too
                    String[] selectedValues = value.split(",");
                    for (String val : selectedValues) {
                        if (!allowedOptions.contains(val.trim())) {
                            throw new IllegalArgumentException("Value '" + val + "' is not a valid option for " + attribute.getName());
                        }
                    }
                }
            }
        }
    }
}
