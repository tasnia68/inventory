package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateStorageLocationRequest;
import com.inventory.system.payload.StorageLocationDto;
import com.inventory.system.service.StorageLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage-locations")
@RequiredArgsConstructor
public class StorageLocationController {

    private final StorageLocationService storageLocationService;

    @PostMapping
    public ResponseEntity<ApiResponse<StorageLocationDto>> createStorageLocation(@Valid @RequestBody CreateStorageLocationRequest request) {
        StorageLocationDto location = storageLocationService.createStorageLocation(request);
        ApiResponse<StorageLocationDto> response = new ApiResponse<>(true, "Storage location created successfully", location);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StorageLocationDto>> getStorageLocationById(@PathVariable UUID id) {
        StorageLocationDto location = storageLocationService.getStorageLocationById(id);
        ApiResponse<StorageLocationDto> response = new ApiResponse<>(true, "Storage location retrieved successfully", location);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StorageLocationDto>>> getStorageLocationsByWarehouse(@RequestParam UUID warehouseId) {
        List<StorageLocationDto> locations = storageLocationService.getStorageLocationsByWarehouse(warehouseId);
        ApiResponse<List<StorageLocationDto>> response = new ApiResponse<>(true, "Storage locations retrieved successfully", locations);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStorageLocation(@PathVariable UUID id) {
        storageLocationService.deleteStorageLocation(id);
        ApiResponse<Void> response = new ApiResponse<>(true, "Storage location deleted successfully", null);
        return ResponseEntity.ok(response);
    }
}
