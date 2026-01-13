package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.UnitOfMeasureDto;
import com.inventory.system.service.UnitOfMeasureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uoms")
@RequiredArgsConstructor
public class UnitOfMeasureController {

    private final UnitOfMeasureService uomService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UnitOfMeasureDto>> createUom(@Valid @RequestBody UnitOfMeasureDto uomDto) {
        UnitOfMeasureDto createdUom = uomService.createUom(uomDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Unit of Measure created successfully", createdUom), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UnitOfMeasureDto>> updateUom(@PathVariable UUID id, @Valid @RequestBody UnitOfMeasureDto uomDto) {
        UnitOfMeasureDto updatedUom = uomService.updateUom(id, uomDto);
        return new ResponseEntity<>(new ApiResponse<>(true, "Unit of Measure updated successfully", updatedUom), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUom(@PathVariable UUID id) {
        uomService.deleteUom(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Unit of Measure deleted successfully", null), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<UnitOfMeasureDto>> getUom(@PathVariable UUID id) {
        UnitOfMeasureDto uom = uomService.getUom(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Unit of Measure retrieved successfully", uom), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<UnitOfMeasureDto>>> getAllUoms() {
        List<UnitOfMeasureDto> uoms = uomService.getAllUoms();
        return new ResponseEntity<>(new ApiResponse<>(true, "Units of Measure retrieved successfully", uoms), HttpStatus.OK);
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<BigDecimal>> convert(
            @RequestParam BigDecimal value,
            @RequestParam UUID fromUomId,
            @RequestParam UUID toUomId) {
        BigDecimal result = uomService.convert(value, fromUomId, toUomId);
        return new ResponseEntity<>(new ApiResponse<>(true, "Conversion successful", result), HttpStatus.OK);
    }
}
