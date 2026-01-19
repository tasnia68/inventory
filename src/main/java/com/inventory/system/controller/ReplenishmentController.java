package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ReplenishmentRuleDto;
import com.inventory.system.payload.ReplenishmentSuggestionDto;
import com.inventory.system.service.ReplenishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/replenishment")
@RequiredArgsConstructor
public class ReplenishmentController {

    private final ReplenishmentService replenishmentService;

    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<ReplenishmentRuleDto>> createRule(@Valid @RequestBody ReplenishmentRuleDto dto) {
        ReplenishmentRuleDto rule = replenishmentService.createRule(dto);
        return ResponseEntity.ok(ApiResponse.success(rule, "Replenishment rule created successfully"));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<ReplenishmentRuleDto>> updateRule(@PathVariable UUID id, @Valid @RequestBody ReplenishmentRuleDto dto) {
        ReplenishmentRuleDto rule = replenishmentService.updateRule(id, dto);
        return ResponseEntity.ok(ApiResponse.success(rule, "Replenishment rule updated successfully"));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        replenishmentService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Replenishment rule deleted successfully"));
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<ReplenishmentRuleDto>> getRule(@PathVariable UUID id) {
        ReplenishmentRuleDto rule = replenishmentService.getRule(id);
        return ResponseEntity.ok(ApiResponse.success(rule, "Replenishment rule retrieved successfully"));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<ReplenishmentRuleDto>>> getRulesByWarehouse(@RequestParam UUID warehouseId) {
        List<ReplenishmentRuleDto> rules = replenishmentService.getRulesByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(rules, "Replenishment rules retrieved successfully"));
    }

    @PostMapping("/rules/{id}/calculate")
    public ResponseEntity<ApiResponse<Void>> calculateReorderPoint(@PathVariable UUID id) {
        replenishmentService.calculateReorderPoint(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Reorder point calculation triggered successfully"));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<ReplenishmentSuggestionDto>>> getReplenishmentSuggestions(@RequestParam UUID warehouseId) {
        List<ReplenishmentSuggestionDto> suggestions = replenishmentService.getReplenishmentSuggestions(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(suggestions, "Replenishment suggestions retrieved successfully"));
    }
}
