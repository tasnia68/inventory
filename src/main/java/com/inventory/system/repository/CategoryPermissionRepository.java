package com.inventory.system.repository;

import com.inventory.system.common.entity.CategoryPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryPermissionRepository extends JpaRepository<CategoryPermission, UUID> {
    List<CategoryPermission> findByCategoryId(UUID categoryId);
    void deleteByCategoryId(UUID categoryId);
}