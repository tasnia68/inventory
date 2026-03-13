package com.inventory.system.service;

import com.inventory.system.payload.CreateWarehouseRequest;
import com.inventory.system.payload.UpdateWarehouseRequest;
import com.inventory.system.payload.WarehouseCapacityDto;
import com.inventory.system.payload.WarehouseCapacityUpdateRequest;
import com.inventory.system.payload.WarehouseDto;

import java.util.List;
import java.util.UUID;

public interface WarehouseService {
    WarehouseDto createWarehouse(CreateWarehouseRequest request);
    WarehouseDto getWarehouseById(UUID id);
    List<WarehouseDto> getAllWarehouses();
    WarehouseDto updateWarehouse(UUID id, UpdateWarehouseRequest request);
    void deleteWarehouse(UUID id);

    WarehouseCapacityDto getWarehouseCapacity(UUID id);
    WarehouseCapacityDto updateWarehouseCapacity(UUID id, WarehouseCapacityUpdateRequest request);
}
