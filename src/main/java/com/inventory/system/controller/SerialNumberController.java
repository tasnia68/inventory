package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.SerialNumberDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.SerialNumberStatus;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SerialNumberRepository;
import com.inventory.system.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/serial-numbers")
@RequiredArgsConstructor
public class SerialNumberController {

    private final StockService stockService;
    private final SerialNumberRepository serialNumberRepository;
    private final ProductVariantRepository productVariantRepository;

    @GetMapping("/{serialNumber}/history")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getHistory(@PathVariable String serialNumber) {
        List<StockMovementDto> history = stockService.getSerialNumberHistory(serialNumber);
        ApiResponse<List<StockMovementDto>> response = new ApiResponse<>(true, "Serial number history retrieved successfully", history);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> getSerialNumbers(
            @RequestParam UUID productVariantId,
            @RequestParam(required = false) SerialNumberStatus status) {
        ProductVariant variant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", productVariantId));

        List<SerialNumber> serials = (status == null)
            ? serialNumberRepository.findByProductVariantId(productVariantId)
            : serialNumberRepository.findByProductVariantAndStatus(variant, status);

        List<SerialNumberDto> dtos = serials.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos, "Serial numbers retrieved successfully"));
    }

    private SerialNumberDto mapToDto(SerialNumber serial) {
        SerialNumberDto dto = new SerialNumberDto();
        dto.setId(serial.getId());
        dto.setSerialNumber(serial.getSerialNumber());
        dto.setProductVariantId(serial.getProductVariant().getId());
        dto.setProductVariantSku(serial.getProductVariant().getSku());
        if (serial.getWarehouse() != null) {
            dto.setWarehouseId(serial.getWarehouse().getId());
            dto.setWarehouseName(serial.getWarehouse().getName());
        }
        if (serial.getStorageLocation() != null) {
            dto.setStorageLocationId(serial.getStorageLocation().getId());
            dto.setStorageLocationName(serial.getStorageLocation().getName());
        }
        if (serial.getBatch() != null) {
            dto.setBatchId(serial.getBatch().getId());
            dto.setBatchNumber(serial.getBatch().getBatchNumber());
        }
        dto.setStatus(serial.getStatus());
        dto.setCreatedAt(serial.getCreatedAt());
        dto.setUpdatedAt(serial.getUpdatedAt());
        return dto;
    }
}
