package com.inventory.system.controller;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.BatchDto;
import com.inventory.system.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
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
