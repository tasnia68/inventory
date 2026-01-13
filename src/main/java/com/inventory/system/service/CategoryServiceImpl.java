package com.inventory.system.service;

import com.inventory.system.common.entity.Category;
import com.inventory.system.common.entity.ProductAttribute;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CategoryDto;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.ProductAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductAttributeRepository productAttributeRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", dto.getParentId()));
            category.setParent(parent);
        }

        if (dto.getAttributeIds() != null && !dto.getAttributeIds().isEmpty()) {
            List<ProductAttribute> attributes = productAttributeRepository.findAllById(dto.getAttributeIds());
            category.setAttributes(new HashSet<>(attributes));
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToDto(savedCategory, false);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(UUID id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            // Check for circular dependency
            if (isDescendant(dto.getParentId(), id)) {
                throw new IllegalArgumentException("Cannot set a descendant category as parent (circular dependency)");
            }

            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", dto.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        if (dto.getAttributeIds() != null) {
            List<ProductAttribute> attributes = productAttributeRepository.findAllById(dto.getAttributeIds());
            category.setAttributes(new HashSet<>(attributes));
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToDto(updatedCategory, false);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToDto(category, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> mapToDto(cat, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        return rootCategories.stream()
                .map(cat -> mapToDto(cat, true))
                .collect(Collectors.toList());
    }

    private boolean isDescendant(UUID potentialParentId, UUID categoryId) {
        // Find the potential parent
        Category potentialParent = categoryRepository.findById(potentialParentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", potentialParentId));

        // Check up the chain from potentialParent
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private CategoryDto mapToDto(Category category, boolean includeChildren) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());

        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
        }

        Set<UUID> attributeIds = category.getAttributes().stream()
                .map(ProductAttribute::getId)
                .collect(Collectors.toSet());
        dto.setAttributeIds(attributeIds);

        if (includeChildren && category.getChildren() != null) {
            List<CategoryDto> childrenDtos = category.getChildren().stream()
                    .map(child -> mapToDto(child, true))
                    .collect(Collectors.toList());
            dto.setChildren(childrenDtos);
        }

        return dto;
    }
}
