package com.inventory.system.controller;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.BatchDto;
import com.inventory.system.payload.UpdateBatchExpiryRequest;
import com.inventory.system.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchRepository batchRepository;

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<BatchDto>>> getExpiringBatches(
            @RequestParam(defaultValue = "30") int days) {

        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days);

        List<Batch> batches = batchRepository.findByExpiryDateBetween(today, targetDate);

        List<BatchDto> dtos = batches.stream().map(this::mapToDto).collect(Collectors.toList());

        ApiResponse<List<BatchDto>> response = new ApiResponse<>(true, "Expiring batches retrieved successfully", dtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<BatchDto>>> getExpiredBatches() {
        LocalDate today = LocalDate.now();
        List<Batch> batches = batchRepository.findByExpiryDateBefore(today);
        List<BatchDto> dtos = batches.stream().map(this::mapToDto).collect(Collectors.toList());
        ApiResponse<List<BatchDto>> response = new ApiResponse<>(true, "Expired batches retrieved successfully", dtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchDto>> getBatchById(@PathVariable UUID id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));
        ApiResponse<BatchDto> response = new ApiResponse<>(true, "Batch retrieved successfully", mapToDto(batch));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchDto>>> getBatchesByProductVariant(@RequestParam UUID productVariantId) {
        List<Batch> batches = batchRepository.findByProductVariantId(productVariantId);
        List<BatchDto> dtos = batches.stream().map(this::mapToDto).collect(Collectors.toList());
        ApiResponse<List<BatchDto>> response = new ApiResponse<>(true, "Batches retrieved successfully", dtos);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/expiry")
    public ResponseEntity<ApiResponse<BatchDto>> updateBatchExpiry(
            @PathVariable UUID id,
            @RequestBody UpdateBatchExpiryRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (request.getManufacturingDate() != null) {
            batch.setManufacturingDate(request.getManufacturingDate());
        }
        if (request.getExpiryDate() != null) {
            batch.setExpiryDate(request.getExpiryDate());
        }

        Batch saved = batchRepository.save(batch);
        ApiResponse<BatchDto> response = new ApiResponse<>(true, "Batch expiry updated successfully", mapToDto(saved));
        return ResponseEntity.ok(response);
    }

    private BatchDto mapToDto(Batch batch) {
        BatchDto dto = new BatchDto();
        dto.setId(batch.getId());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setManufacturingDate(batch.getManufacturingDate());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setProductVariantId(batch.getProductVariant().getId());
        return dto;
    }
}
