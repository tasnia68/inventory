package com.inventory.system.service;

import com.inventory.system.payload.CreateStorageLocationRequest;
import com.inventory.system.payload.StorageLocationDto;

import java.util.List;
import java.util.UUID;

public interface StorageLocationService {
    StorageLocationDto createStorageLocation(CreateStorageLocationRequest request);
    StorageLocationDto getStorageLocationById(UUID id);
    List<StorageLocationDto> getStorageLocationsByWarehouse(UUID warehouseId);
    void deleteStorageLocation(UUID id);
}
