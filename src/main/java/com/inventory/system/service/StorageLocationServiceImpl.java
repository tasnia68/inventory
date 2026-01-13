package com.inventory.system.service;

import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateStorageLocationRequest;
import com.inventory.system.payload.StorageLocationDto;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageLocationServiceImpl implements StorageLocationService {

    private final StorageLocationRepository storageLocationRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public StorageLocationDto createStorageLocation(CreateStorageLocationRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setName(request.getName());
        storageLocation.setType(request.getType());
        storageLocation.setWarehouse(warehouse);

        if (request.getParentId() != null) {
            StorageLocation parent = storageLocationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent location not found with id: " + request.getParentId()));

            if (!parent.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Parent location must belong to the same warehouse");
            }

            storageLocation.setParent(parent);
        }

        StorageLocation savedLocation = storageLocationRepository.save(storageLocation);
        return mapToDto(savedLocation);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageLocationDto getStorageLocationById(UUID id) {
        StorageLocation storageLocation = storageLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage location not found with id: " + id));
        return mapToDto(storageLocation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocationDto> getStorageLocationsByWarehouse(UUID warehouseId) {
        return storageLocationRepository.findByWarehouseId(warehouseId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStorageLocation(UUID id) {
        StorageLocation storageLocation = storageLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage location not found with id: " + id));
        storageLocationRepository.delete(storageLocation);
    }

    private StorageLocationDto mapToDto(StorageLocation location) {
        StorageLocationDto dto = new StorageLocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setType(location.getType());
        dto.setWarehouseId(location.getWarehouse().getId());
        dto.setWarehouseName(location.getWarehouse().getName());
        if (location.getParent() != null) {
            dto.setParentId(location.getParent().getId());
        }
        if (location.getChildren() != null && !location.getChildren().isEmpty()) {
            dto.setChildren(location.getChildren().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
