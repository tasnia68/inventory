package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.RmaDto;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rmas")
@RequiredArgsConstructor
public class RmaController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<RmaDto>> createRma(@Valid @RequestBody CreateRmaRequest request) {
        RmaDto rma = shipmentService.createRma(request);
        return new ResponseEntity<>(ApiResponse.success(rma, "RMA created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RmaDto>> getRma(@PathVariable UUID id) {
        RmaDto rma = shipmentService.getRma(id);
        return ResponseEntity.ok(ApiResponse.success(rma, "RMA retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RmaDto>>> getAllRmas(
            @RequestParam(required = false) UUID salesOrderId,
            @RequestParam(required = false) String rmaNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<RmaDto> rmas = shipmentService.getAllRmas(salesOrderId, rmaNumber, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(rmas, "RMAs retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RmaDto>> updateRmaStatus(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateRmaStatusRequest request) {
        RmaDto rma = shipmentService.updateRmaStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(rma, "RMA status updated successfully"));
    }
}