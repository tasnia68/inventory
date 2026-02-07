package com.inventory.system.service;

import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StockService {
    Page<StockDto> getStocks(UUID warehouseId, UUID productVariantId, Pageable pageable);
    Page<StockDto> searchStocks(String query, Pageable pageable);
    StockDto getStock(UUID id);
    StockMovementDto adjustStock(StockAdjustmentDto adjustmentDto);
    Page<StockMovementDto> getStockMovements(UUID warehouseId, UUID productVariantId, Pageable pageable);
    List<StockMovementDto> getSerialNumberHistory(String serialNumber);
}
