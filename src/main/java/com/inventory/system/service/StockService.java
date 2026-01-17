package com.inventory.system.service;

import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockService {
    Page<StockDto> getStocks(UUID warehouseId, UUID productVariantId, Pageable pageable);
    StockDto getStock(UUID id);
    StockDto adjustStock(StockAdjustmentDto adjustmentDto);
    Page<StockMovementDto> getStockMovements(UUID warehouseId, UUID productVariantId, Pageable pageable);
}
