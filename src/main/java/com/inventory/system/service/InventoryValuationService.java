package com.inventory.system.service;

import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.payload.InventoryValuationReportDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryValuationService {
    void processInbound(StockMovement movement, BigDecimal unitCost);
    void processOutbound(StockMovement movement);
    BigDecimal getCurrentValuation(UUID productVariantId, UUID warehouseId);
    List<InventoryValuationReportDto> getValuationReport(UUID warehouseId);
}
