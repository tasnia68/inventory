package com.inventory.system.service;

import com.inventory.system.common.entity.InventoryValuationLayer;
import com.inventory.system.common.entity.ProductCost;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.ValuationMethod;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.payload.InventoryValuationReportDto;
import com.inventory.system.repository.InventoryValuationLayerRepository;
import com.inventory.system.repository.ProductCostRepository;
import com.inventory.system.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryValuationServiceImpl implements InventoryValuationService {

    private final InventoryValuationLayerRepository layerRepository;
    private final ProductCostRepository productCostRepository;
    private final TenantSettingService tenantSettingService;
    private final StockRepository stockRepository;

    private static final String VALUATION_METHOD_KEY = "INVENTORY_VALUATION_METHOD";

    @Override
    @Transactional
    public void processInbound(StockMovement movement, BigDecimal unitCost) {
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }

        ValuationMethod method = getValuationMethod();

        if (method == ValuationMethod.WEIGHTED_AVERAGE) {
            processWeightedAverageInbound(movement, unitCost);
        } else {
            processLayeredInbound(movement, unitCost);
        }

        movement.setUnitCost(unitCost);
        movement.setTotalCost(unitCost.multiply(movement.getQuantity()));
    }

    @Override
    @Transactional
    public void processOutbound(StockMovement movement) {
        ValuationMethod method = getValuationMethod();

        if (method == ValuationMethod.WEIGHTED_AVERAGE) {
            processWeightedAverageOutbound(movement);
        } else {
            processLayeredOutbound(movement, method);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentValuation(UUID productVariantId, UUID warehouseId) {
        ValuationMethod method = getValuationMethod();
        if (method == ValuationMethod.WEIGHTED_AVERAGE) {
            // For Weighted Average, we return the unit cost.
             return productCostRepository.findByProductVariantIdAndWarehouseId(productVariantId, warehouseId)
                    .map(ProductCost::getAverageCost)
                    .orElse(BigDecimal.ZERO);
        } else {
            BigDecimal value = layerRepository.calculateTotalValue(productVariantId, warehouseId);
            return value != null ? value : BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryValuationReportDto> getValuationReport(UUID warehouseId) {
        List<Stock> stocks;
        if (warehouseId != null) {
            stocks = stockRepository.findAll((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
        } else {
            stocks = stockRepository.findAll();
        }

        // Group by Warehouse and ProductVariant
        Map<String, List<Stock>> grouped = stocks.stream()
                .collect(Collectors.groupingBy(s -> s.getWarehouse().getId() + ":" + s.getProductVariant().getId()));

        List<InventoryValuationReportDto> report = new ArrayList<>();
        ValuationMethod method = getValuationMethod();

        for (List<Stock> group : grouped.values()) {
            if (group.isEmpty()) continue;

            Stock first = group.get(0);
            UUID pId = first.getProductVariant().getId();
            UUID wId = first.getWarehouse().getId();

            BigDecimal totalQty = group.stream()
                    .map(Stock::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalQty.compareTo(BigDecimal.ZERO) == 0) continue;

            InventoryValuationReportDto dto = new InventoryValuationReportDto();
            dto.setProductVariantId(pId);
            dto.setProductVariantSku(first.getProductVariant().getSku());
            dto.setProductName(first.getProductVariant().getTemplate().getName());
            dto.setWarehouseId(wId);
            dto.setWarehouseName(first.getWarehouse().getName());
            dto.setQuantity(totalQty);

            if (method == ValuationMethod.WEIGHTED_AVERAGE) {
                BigDecimal avgCost = getCurrentValuation(pId, wId);
                dto.setUnitCost(avgCost);
                dto.setTotalValue(totalQty.multiply(avgCost));
            } else {
                // FIFO/LIFO: Value comes from layers
                BigDecimal totalValue = getCurrentValuation(pId, wId);
                dto.setTotalValue(totalValue);
                // Approx Unit Cost
                if (totalQty.compareTo(BigDecimal.ZERO) != 0) {
                    dto.setUnitCost(totalValue.divide(totalQty, 6, RoundingMode.HALF_UP));
                } else {
                    dto.setUnitCost(BigDecimal.ZERO);
                }
            }
            report.add(dto);
        }
        return report;
    }

    private void processWeightedAverageInbound(StockMovement movement, BigDecimal unitCost) {
        UUID variantId = movement.getProductVariant().getId();
        UUID warehouseId = movement.getWarehouse().getId();

        ProductCost productCost = productCostRepository.findByProductVariantIdAndWarehouseId(variantId, warehouseId)
                .orElseGet(() -> {
                    ProductCost pc = new ProductCost();
                    pc.setProductVariant(movement.getProductVariant());
                    pc.setWarehouse(movement.getWarehouse());
                    pc.setAverageCost(BigDecimal.ZERO);
                    return pc;
                });

        // Get total stock quantity for this product in this warehouse
        // Since Stock is split by StorageLocation, we need to sum them up.
        // Also note that StockService has ALREADY updated the stock quantity for this transaction.
        // So currentTotalStock includes the inbound quantity.

        // We need a way to sum stock by variant and warehouse.
        // StockRepository should have a method for this, or we use findAll with spec.
        // But we can just use the sum query.

        // Assuming we add a method to StockRepository or use logic here.
        // Let's rely on calculating it.

        BigDecimal currentTotalStock = stockRepository.countTotalQuantityByProductVariantAndWarehouse(variantId, warehouseId);

        if (currentTotalStock == null) currentTotalStock = BigDecimal.ZERO;

        BigDecimal inboundQty = movement.getQuantity();
        BigDecimal oldTotalStock = currentTotalStock.subtract(inboundQty);

        if (oldTotalStock.compareTo(BigDecimal.ZERO) <= 0) {
            // First stock or negative stock scenario
            productCost.setAverageCost(unitCost);
        } else {
            BigDecimal oldCost = productCost.getAverageCost();
            BigDecimal totalValue = (oldTotalStock.multiply(oldCost)).add(inboundQty.multiply(unitCost));
            BigDecimal newAvgCost = totalValue.divide(currentTotalStock, 6, RoundingMode.HALF_UP);
            productCost.setAverageCost(newAvgCost);
        }

        productCostRepository.save(productCost);
    }

    private void processLayeredInbound(StockMovement movement, BigDecimal unitCost) {
        InventoryValuationLayer layer = new InventoryValuationLayer();
        layer.setProductVariant(movement.getProductVariant());
        layer.setWarehouse(movement.getWarehouse());
        layer.setQuantityRemaining(movement.getQuantity());
        layer.setUnitCost(unitCost);
        layer.setReceivedDate(LocalDateTime.now());
        layer.setReference(movement.getReferenceId());
        layerRepository.save(layer);
    }

    private void processWeightedAverageOutbound(StockMovement movement) {
        ProductCost cost = productCostRepository.findByProductVariantIdAndWarehouseId(
                movement.getProductVariant().getId(), movement.getWarehouse().getId())
                .orElse(null);

        BigDecimal avgCost = (cost != null) ? cost.getAverageCost() : BigDecimal.ZERO;

        movement.setUnitCost(avgCost);
        movement.setTotalCost(avgCost.multiply(movement.getQuantity()));
    }

    private void processLayeredOutbound(StockMovement movement, ValuationMethod method) {
        List<InventoryValuationLayer> layers;
        if (method == ValuationMethod.LIFO) {
            layers = layerRepository.findLayersForLifo(movement.getProductVariant().getId(), movement.getWarehouse().getId());
        } else {
            layers = layerRepository.findLayersForFifo(movement.getProductVariant().getId(), movement.getWarehouse().getId());
        }

        BigDecimal remainingQty = movement.getQuantity();
        BigDecimal totalCost = BigDecimal.ZERO;

        for (InventoryValuationLayer layer : layers) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal consume = remainingQty.min(layer.getQuantityRemaining());

            BigDecimal layerCost = consume.multiply(layer.getUnitCost());
            totalCost = totalCost.add(layerCost);

            layer.setQuantityRemaining(layer.getQuantityRemaining().subtract(consume));
            remainingQty = remainingQty.subtract(consume);

            layerRepository.save(layer);
        }

        // If we ran out of layers (selling more than we have in history?), we assume 0 cost for remainder?
        // Or we use the last layer's cost?
        // For now, let's assume 0 cost for uncovered stock (shouldn't happen if stock check is correct).

        // Calculate average unit cost for this transaction
        BigDecimal unitCost = BigDecimal.ZERO;
        if (movement.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            unitCost = totalCost.divide(movement.getQuantity(), 6, RoundingMode.HALF_UP);
        }

        movement.setUnitCost(unitCost);
        movement.setTotalCost(totalCost);
    }

    private ValuationMethod getValuationMethod() {
        try {
            String methodStr = tenantSettingService.getSetting(VALUATION_METHOD_KEY).getValue();
            return ValuationMethod.valueOf(methodStr);
        } catch (Exception e) {
            return ValuationMethod.FIFO; // Default
        }
    }
}
