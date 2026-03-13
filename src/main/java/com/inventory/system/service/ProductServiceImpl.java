package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final ProductVariantVersionRepository productVariantVersionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String generateSku(ProductVariant variant) {
        // Simple SKU generation logic: TEMPLATE_NAME-RANDOM_UUID_SUBSTRING
        // In a real scenario, this would be more complex, possibly involving
        // attributes.
        String templateName = variant.getTemplate() != null ? variant.getTemplate().getName() : "UNK";
        String prefix = templateName.length() > 3 ? templateName.substring(0, 3).toUpperCase()
                : templateName.toUpperCase();
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

        if (productTemplateDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(productTemplateDto.getCategoryId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Category", "id", productTemplateDto.getCategoryId()));
            template.setCategory(category);
        }

        if (productTemplateDto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(productTemplateDto.getUomId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("UnitOfMeasure", "id", productTemplateDto.getUomId()));
            template.setUom(uom);
        }

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

        if (productTemplateDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(productTemplateDto.getCategoryId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Category", "id", productTemplateDto.getCategoryId()));
            template.setCategory(category);
        }

        if (productTemplateDto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(productTemplateDto.getUomId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("UnitOfMeasure", "id", productTemplateDto.getUomId()));
            template.setUom(uom);
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
        if (dto.getType() != null)
            attribute.setType(dto.getType());
        if (dto.getRequired() != null)
            attribute.setRequired(dto.getRequired());
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
        List<ProductAttributeValue> values = productAttributeValueRepository.findByAttributeId(id);
        if (!values.isEmpty()) {
            productAttributeValueRepository.deleteAll(values);
        }
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
        String barcode = dto.getBarcode();
        if (barcode != null && barcode.isBlank()) {
            barcode = null;
        }
        variant.setBarcode(barcode);

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
                        .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id",
                                valDto.getAttributeId()));

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

        ProductVariantDto result = mapToDto(variant);
        saveVariantVersion(variant, "CREATE");
        return result;
    }

    @Override
    @Transactional
    public ProductVariantDto updateProductVariant(UUID id, ProductVariantDto dto) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant", "id", id));

        boolean templateChanged = false;
        if (dto.getTemplateId() != null && (variant.getTemplate() == null
                || !dto.getTemplateId().equals(variant.getTemplate().getId()))) {
            ProductTemplate template = productTemplateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", dto.getTemplateId()));
            variant.setTemplate(template);
            templateChanged = true;
        }

        if (dto.getSku() != null && !dto.getSku().isBlank()) {
            if (!dto.getSku().equals(variant.getSku())) {
                if (productVariantRepository.existsBySku(dto.getSku())) {
                    throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
                }
                variant.setSku(dto.getSku());
            }
        }

        if (dto.getBarcode() != null) {
            String barcode = dto.getBarcode();
            if (barcode.isBlank()) {
                barcode = null;
            }
            variant.setBarcode(barcode);
        }

        if (dto.getPrice() != null) {
            variant.setPrice(dto.getPrice());
        }

        if (dto.getAttributeValues() != null) {
            List<ProductAttributeValue> existingValues = productAttributeValueRepository.findByVariantId(id);
            if (!existingValues.isEmpty()) {
                productAttributeValueRepository.deleteAll(existingValues);
            }

            List<ProductAttributeValue> attributeValues = new ArrayList<>();
            for (ProductVariantDto.AttributeValueDto valDto : dto.getAttributeValues()) {
                ProductAttribute attribute = productAttributeRepository.findById(valDto.getAttributeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product Attribute", "id",
                                valDto.getAttributeId()));

                validateAttributeValue(attribute, valDto.getValue());

                ProductAttributeValue attributeValue = new ProductAttributeValue();
                attributeValue.setVariant(variant);
                attributeValue.setAttribute(attribute);
                attributeValue.setValue(valDto.getValue());

                attributeValues.add(attributeValue);
            }

            // Validate required attributes
            UUID templateId = variant.getTemplate() != null ? variant.getTemplate().getId() : null;
            if (templateId != null) {
                List<ProductAttribute> requiredAttributes = productAttributeRepository.findByTemplateId(templateId)
                        .stream().filter(ProductAttribute::getRequired).collect(Collectors.toList());

                List<UUID> providedAttributeIds = dto.getAttributeValues().stream()
                        .map(ProductVariantDto.AttributeValueDto::getAttributeId).collect(Collectors.toList());

                for (ProductAttribute required : requiredAttributes) {
                    if (!providedAttributeIds.contains(required.getId())) {
                        throw new IllegalArgumentException("Missing required attribute: " + required.getName());
                    }
                }
            }

            productAttributeValueRepository.saveAll(attributeValues);
            variant.setAttributeValues(attributeValues);
        } else if (templateChanged) {
            throw new IllegalArgumentException("Attribute values are required when changing template");
        }

        ProductVariant updated = productVariantRepository.save(variant);
        ProductVariantDto result = mapToDto(updated);
        saveVariantVersion(updated, "UPDATE");
        return result;
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
    @Transactional(readOnly = true)
    public Page<ProductVariantDto> getProductVariantsByCategory(UUID categoryId, Pageable pageable) {
        return productVariantRepository.findByTemplateCategoryId(categoryId, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantDto> getProductVariantsByTemplate(UUID templateId) {
        return productVariantRepository.findByTemplateId(templateId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantDto> searchProductVariants(String query, Pageable pageable) {
        return productVariantRepository.searchByQuery(query, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantDto> searchProductVariants(ProductSearchRequest request, Pageable pageable) {
        return productVariantRepository.searchAdvanced(
                        request.getQ(),
                        request.getCategoryId(),
                        request.getTemplateId(),
                        request.getAttributeId(),
                        request.getAttributeValue(),
                        pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void deleteProductVariant(UUID id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant", "id", id));
        saveVariantVersion(variant, "DELETE");
        productVariantRepository.delete(variant);
    }

    @Override
    @Transactional
    public BulkProductOperationResultDto bulkOperateProducts(BulkProductOperationRequest request) {
        if (request.getProductVariantIds() == null || request.getProductVariantIds().isEmpty()) {
            throw new BadRequestException("productVariantIds cannot be empty");
        }

        List<ProductVariant> variants = productVariantRepository.findAllById(request.getProductVariantIds());
        BulkProductOperationResultDto result = new BulkProductOperationResultDto();
        result.setTotalRequested(request.getProductVariantIds().size());

        for (ProductVariant variant : variants) {
            try {
                if (request.getPercentagePriceAdjustment() != null) {
                    BigDecimal multiplier = BigDecimal.ONE.add(request.getPercentagePriceAdjustment().divide(new BigDecimal("100")));
                    variant.setPrice(variant.getPrice().multiply(multiplier));
                }
                if (request.getAbsolutePriceAdjustment() != null) {
                    variant.setPrice(variant.getPrice().add(request.getAbsolutePriceAdjustment()));
                }
                if (request.getActive() != null && variant.getTemplate() != null) {
                    variant.getTemplate().setIsActive(request.getActive());
                }

                ProductVariant saved = productVariantRepository.save(variant);
                saveVariantVersion(saved, "BULK_UPDATE");
                result.setTotalUpdated(result.getTotalUpdated() + 1);
            } catch (Exception ex) {
                result.getErrors().add("Variant " + variant.getId() + ": " + ex.getMessage());
            }
        }

        return result;
    }

    @Override
    @Transactional
    public ProductImportResultDto importProductsFromCsv(MultipartFile file) {
        ProductImportResultDto result = new ProductImportResultDto();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerProcessed = false;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (!headerProcessed) {
                    headerProcessed = true;
                    if (line.toLowerCase().contains("sku") && line.toLowerCase().contains("templateid")) {
                        continue;
                    }
                }

                result.setTotalRows(result.getTotalRows() + 1);
                String[] parts = line.split(",", -1);
                if (parts.length < 4) {
                    result.setFailed(result.getFailed() + 1);
                    result.getErrors().add("Invalid row format: " + line);
                    continue;
                }

                try {
                    ProductVariantDto dto = new ProductVariantDto();
                    dto.setSku(parts[0].trim());
                    dto.setBarcode(parts[1].trim().isBlank() ? null : parts[1].trim());
                    dto.setPrice(new BigDecimal(parts[2].trim()));
                    dto.setTemplateId(UUID.fromString(parts[3].trim()));
                    dto.setAttributeValues(new ArrayList<>());

                    createProductVariant(dto);
                    result.setImported(result.getImported() + 1);
                } catch (Exception rowEx) {
                    result.setFailed(result.getFailed() + 1);
                    result.getErrors().add("Row failed: " + line + " | " + rowEx.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV file", e);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportProductsToCsv() {
        List<ProductVariant> variants = productVariantRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("sku,barcode,price,templateId").append("\n");

        for (ProductVariant variant : variants) {
            sb.append(safeCsv(variant.getSku())).append(",")
                    .append(safeCsv(variant.getBarcode())).append(",")
                    .append(variant.getPrice() != null ? variant.getPrice() : "")
                    .append(",")
                    .append(variant.getTemplate() != null ? variant.getTemplate().getId() : "")
                    .append("\n");
        }

        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantVersionDto> getProductVariantHistory(UUID variantId, Pageable pageable) {
        return productVariantVersionRepository.findByProductVariantId(variantId, pageable)
                .map(this::mapToDto);
    }

    // --- Simple Product Method (Convenience API) ---

    @Override
    @Transactional
    public ProductVariantDto createSimpleProduct(SimpleProductDto dto) {
        // Step 1: Create the template
        ProductTemplateDto templateDto = new ProductTemplateDto();
        templateDto.setName(dto.getName());
        templateDto.setDescription(dto.getDescription());
        templateDto.setCategoryId(dto.getCategoryId());
        templateDto.setUomId(dto.getUomId());
        templateDto.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        ProductTemplateDto createdTemplate = createTemplate(templateDto);

        // Step 2: Create a single variant for this template
        ProductVariantDto variantDto = new ProductVariantDto();
        variantDto.setTemplateId(createdTemplate.getId());
        variantDto.setSku(dto.getSku());
        variantDto.setBarcode(dto.getBarcode());
        variantDto.setPrice(dto.getPrice());
        variantDto.setAttributeValues(null); // Simple products don't have attributes

        return createProductVariant(variantDto);
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

        if (template.getCategory() != null) {
            dto.setCategoryId(template.getCategory().getId());
            dto.setCategoryName(template.getCategory().getName());
        }
        if (template.getUom() != null) {
            dto.setUomId(template.getUom().getId());
            dto.setUomName(template.getUom().getName());
        }
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
        if (attribute.getTemplate() != null)
            dto.setTemplateId(attribute.getTemplate().getId());
        if (attribute.getGroup() != null)
            dto.setGroupId(attribute.getGroup().getId());
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
        dto.setBarcode(variant.getBarcode());
        dto.setPrice(variant.getPrice());
        if (variant.getTemplate() != null) {
            dto.setTemplateId(variant.getTemplate().getId());
            setMainImageFields(dto, variant.getTemplate().getId());
        }
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

    private void setMainImageFields(ProductVariantDto dto, UUID templateId) {
        productImageRepository.findByProductTemplateId(templateId).stream()
                .filter(ProductImage::getIsMain)
                .findFirst()
                .ifPresentOrElse(image -> {
                    dto.setMainImageId(image.getId());
                    dto.setMainImageUrl("/api/v1/product-images/" + image.getId() + "/file");
                }, () -> productImageRepository.findByProductTemplateId(templateId).stream()
                        .findFirst()
                        .ifPresent(image -> {
                            dto.setMainImageId(image.getId());
                            dto.setMainImageUrl("/api/v1/product-images/" + image.getId() + "/file");
                        }));
    }

    private void validateAttributeValue(ProductAttribute attribute, String value) {
        if (value == null && attribute.getRequired()) {
            throw new IllegalArgumentException("Attribute " + attribute.getName() + " is required.");
        }

        if (value != null) {
            // Regex validation
            if (attribute.getValidationRegex() != null && !attribute.getValidationRegex().isEmpty()) {
                if (!value.matches(attribute.getValidationRegex())) {
                    throw new IllegalArgumentException("Value '" + value + "' for attribute " + attribute.getName()
                            + " does not match pattern: " + attribute.getValidationRegex());
                }
            }

            // Options validation (DROPDOWN/MULTI_SELECT)
            if ((attribute.getType() == ProductAttribute.AttributeType.DROPDOWN
                    || attribute.getType() == ProductAttribute.AttributeType.MULTI_SELECT)
                    && attribute.getOptions() != null) {
                // Assuming options are comma-separated or JSON list. For simplicity here:
                // comma-separated.
                // In production, robust parsing is needed.
                List<String> allowedOptions = Arrays.stream(attribute.getOptions().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());

                if (attribute.getType() == ProductAttribute.AttributeType.DROPDOWN) {
                    if (!allowedOptions.contains(value)) {
                        throw new IllegalArgumentException(
                                "Value '" + value + "' is not a valid option for " + attribute.getName());
                    }
                } else {
                    // MULTI_SELECT: value might be comma separated too
                    String[] selectedValues = value.split(",");
                    for (String val : selectedValues) {
                        if (!allowedOptions.contains(val.trim())) {
                            throw new IllegalArgumentException(
                                    "Value '" + val + "' is not a valid option for " + attribute.getName());
                        }
                    }
                }
            }
        }
    }

    private void saveVariantVersion(ProductVariant variant, String changeType) {
        int nextVersion = productVariantVersionRepository
                .findTopByProductVariantIdOrderByVersionNumberDesc(variant.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        ProductVariantVersion version = new ProductVariantVersion();
        version.setProductVariantId(variant.getId());
        version.setVersionNumber(nextVersion);
        version.setChangeType(changeType);
        version.setSnapshot(toSnapshot(variant));
        productVariantVersionRepository.save(version);
    }

    private String toSnapshot(ProductVariant variant) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", variant.getId());
        snapshot.put("sku", variant.getSku());
        snapshot.put("barcode", variant.getBarcode());
        snapshot.put("price", variant.getPrice());
        snapshot.put("templateId", variant.getTemplate() != null ? variant.getTemplate().getId() : null);
        snapshot.put("attributeValues", variant.getAttributeValues() == null ? List.of() : variant.getAttributeValues().stream().map(v -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attributeId", v.getAttribute() != null ? v.getAttribute().getId() : null);
            row.put("value", v.getValue());
            return row;
        }).collect(Collectors.toList()));

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"failed_to_serialize_snapshot\"}";
        }
    }

    private ProductVariantVersionDto mapToDto(ProductVariantVersion version) {
        ProductVariantVersionDto dto = new ProductVariantVersionDto();
        dto.setId(version.getId());
        dto.setProductVariantId(version.getProductVariantId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setChangeType(version.getChangeType());
        dto.setSnapshot(version.getSnapshot());
        dto.setCreatedAt(version.getCreatedAt());
        dto.setCreatedBy(version.getCreatedBy());
        return dto;
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
