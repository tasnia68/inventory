package com.inventory.system.service;

import com.inventory.system.payload.CategoryDto;
import com.inventory.system.payload.CategoryPermissionDto;
import com.inventory.system.payload.UpdateCategoryPermissionsRequest;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto dto);
    CategoryDto updateCategory(UUID id, CategoryDto dto);
    void deleteCategory(UUID id);
    CategoryDto getCategory(UUID id);
    List<CategoryDto> getAllCategories();
    List<CategoryDto> getCategoryTree();

    List<CategoryPermissionDto> getCategoryPermissions(UUID categoryId);
    List<CategoryPermissionDto> updateCategoryPermissions(UUID categoryId, UpdateCategoryPermissionsRequest request);
}
