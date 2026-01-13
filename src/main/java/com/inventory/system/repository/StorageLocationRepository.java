package com.inventory.system.repository;

import com.inventory.system.common.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {
    List<StorageLocation> findByWarehouseId(UUID warehouseId);
    List<StorageLocation> findByParentId(UUID parentId);
}
