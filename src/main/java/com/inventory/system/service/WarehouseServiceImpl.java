package com.inventory.system.service;

import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateWarehouseRequest;
import com.inventory.system.payload.UpdateWarehouseRequest;
import com.inventory.system.payload.WarehouseDto;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setType(request.getType());
        warehouse.setContactNumber(request.getContactNumber());
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return mapToDto(savedWarehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return mapToDto(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarehouseDto updateWarehouse(UUID id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        if (request.getName() != null) {
            warehouse.setName(request.getName());
        }
        if (request.getLocation() != null) {
            warehouse.setLocation(request.getLocation());
        }
        if (request.getType() != null) {
            warehouse.setType(request.getType());
        }
        if (request.getContactNumber() != null) {
            warehouse.setContactNumber(request.getContactNumber());
        }
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        return mapToDto(updatedWarehouse);
    }

    @Override
    @Transactional
    public void deleteWarehouse(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        warehouseRepository.delete(warehouse);
    }

    private WarehouseDto mapToDto(Warehouse warehouse) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setLocation(warehouse.getLocation());
        dto.setType(warehouse.getType());
        dto.setContactNumber(warehouse.getContactNumber());
        dto.setIsActive(warehouse.getIsActive());
        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());
        return dto;
    }
}
