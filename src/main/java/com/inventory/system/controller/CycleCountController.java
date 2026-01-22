package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateCycleCountRequest;
import com.inventory.system.payload.CycleCountDto;
import com.inventory.system.payload.CycleCountEntryRequest;
import com.inventory.system.payload.CycleCountItemDto;
import com.inventory.system.payload.UpdateCycleCountRequest;
import com.inventory.system.service.CycleCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cycle-counts")
@RequiredArgsConstructor
public class CycleCountController {

    private final CycleCountService cycleCountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> createCycleCount(@Valid @RequestBody CreateCycleCountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.createCycleCount(request), "Cycle count created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<CycleCountDto>>> getCycleCounts(@PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.getCycleCounts(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> getCycleCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.getCycleCount(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> updateCycleCount(@PathVariable UUID id, @RequestBody UpdateCycleCountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.updateCycleCount(id, request), "Cycle count updated successfully"));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> startCycleCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.startCycleCount(id), "Cycle count started successfully"));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CycleCountItemDto>>> getCycleCountItems(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.getCycleCountItems(id)));
    }

    @PostMapping("/{id}/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<Void>> enterCount(@PathVariable UUID id, @Valid @RequestBody List<CycleCountEntryRequest> entries) {
        cycleCountService.enterCount(id, entries);
        return ResponseEntity.ok(ApiResponse.success(null, "Count entries saved successfully"));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> finishCycleCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.finishCycleCount(id), "Cycle count finished successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CycleCountDto>> approveCycleCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cycleCountService.approveCycleCount(id), "Cycle count approved successfully"));
    }
}
