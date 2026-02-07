package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockDto>>> getStocks(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productVariantId,
            Pageable pageable) {
        Page<StockDto> stocks = stockService.getStocks(warehouseId, productVariantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(stocks, "Stocks retrieved successfully"));
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<Page<StockDto>>> searchStocks(
            @RequestParam("q") String query,
            Pageable pageable) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Query is required", null));
        }
        Page<StockDto> stocks = stockService.searchStocks(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(stocks, "Stocks retrieved successfully"));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<StockMovementDto>> adjustStock(@Valid @RequestBody StockAdjustmentDto adjustmentDto) {
        StockMovementDto movement = stockService.adjustStock(adjustmentDto);
        return ResponseEntity.ok(ApiResponse.success(movement, "Stock adjusted successfully"));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<Page<StockMovementDto>>> getStockMovements(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productVariantId,
            Pageable pageable) {
        Page<StockMovementDto> movements = stockService.getStockMovements(warehouseId, productVariantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(movements, "Stock movements retrieved successfully"));
    }
}
