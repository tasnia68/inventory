package com.inventory.system.service;

import com.inventory.system.common.entity.InventoryValuationLayer;
import com.inventory.system.common.entity.ProductCost;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.entity.ValuationMethod;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.payload.InventoryValuationReportDto;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.repository.InventoryValuationLayerRepository;
import com.inventory.system.repository.ProductCostRepository;
import com.inventory.system.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InventoryValuationServiceImplTest {

    @Mock
    private InventoryValuationLayerRepository layerRepository;

    @Mock
    private ProductCostRepository productCostRepository;

    @Mock
    private TenantSettingService tenantSettingService;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private InventoryValuationServiceImpl valuationService;

    private StockMovement movement;
    private ProductVariant productVariant;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        productVariant = new ProductVariant();
        productVariant.setId(UUID.randomUUID());
        productVariant.setSku("SKU-1");
        ProductTemplate template = new ProductTemplate();
        template.setName("Product 1");
        productVariant.setTemplate(template);

        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("WH-1");

        movement = new StockMovement();
        movement.setProductVariant(productVariant);
        movement.setWarehouse(warehouse);
        movement.setQuantity(new BigDecimal("10"));
        movement.setReferenceId("REF-1");
    }

    @Test
    void processInbound_FIFO() {
        TenantSettingDto setting = TenantSettingDto.builder().value("FIFO").build();
        when(tenantSettingService.getSetting("INVENTORY_VALUATION_METHOD")).thenReturn(setting);

        valuationService.processInbound(movement, new BigDecimal("100"));

        verify(layerRepository).save(any(InventoryValuationLayer.class));
        assertEquals(new BigDecimal("100"), movement.getUnitCost());
        assertEquals(new BigDecimal("1000"), movement.getTotalCost());
    }

    @Test
    void processInbound_WeightedAverage() {
        TenantSettingDto setting = TenantSettingDto.builder().value("WEIGHTED_AVERAGE").build();
        when(tenantSettingService.getSetting("INVENTORY_VALUATION_METHOD")).thenReturn(setting);

        // Mock ProductCost (Existing: 10 units @ 50)
        ProductCost cost = new ProductCost();
        cost.setAverageCost(new BigDecimal("50"));
        when(productCostRepository.findByProductVariantIdAndWarehouseId(productVariant.getId(), warehouse.getId()))
                .thenReturn(Optional.of(cost));

        // Mock Stock Quantity (Existing 10 + New 10 = 20)
        when(stockRepository.countTotalQuantityByProductVariantAndWarehouse(productVariant.getId(), warehouse.getId()))
                .thenReturn(new BigDecimal("20"));

        // New Inbound: 10 units @ 100
        // Old: 10 * 50 = 500
        // New: 10 * 100 = 1000
        // Total Value: 1500
        // Total Qty: 20
        // New Avg: 75

        valuationService.processInbound(movement, new BigDecimal("100"));

        assertEquals(new BigDecimal("75.000000"), cost.getAverageCost());
        verify(productCostRepository).save(cost);
        assertEquals(new BigDecimal("100"), movement.getUnitCost());
        assertEquals(new BigDecimal("1000"), movement.getTotalCost());
    }

    @Test
    void processOutbound_FIFO() {
        TenantSettingDto setting = TenantSettingDto.builder().value("FIFO").build();
        when(tenantSettingService.getSetting("INVENTORY_VALUATION_METHOD")).thenReturn(setting);

        // Layers: 5 @ 50, 10 @ 60
        InventoryValuationLayer layer1 = new InventoryValuationLayer();
        layer1.setQuantityRemaining(new BigDecimal("5"));
        layer1.setUnitCost(new BigDecimal("50"));

        InventoryValuationLayer layer2 = new InventoryValuationLayer();
        layer2.setQuantityRemaining(new BigDecimal("10"));
        layer2.setUnitCost(new BigDecimal("60"));

        when(layerRepository.findLayersForFifo(productVariant.getId(), warehouse.getId()))
                .thenReturn(List.of(layer1, layer2));

        // Outbound: 8 units
        // Consume: 5 @ 50 (250) + 3 @ 60 (180) = 430
        // Avg Unit Cost = 430 / 8 = 53.75
        movement.setQuantity(new BigDecimal("8"));

        valuationService.processOutbound(movement);

        assertEquals(BigDecimal.ZERO, layer1.getQuantityRemaining());
        assertEquals(new BigDecimal("7"), layer2.getQuantityRemaining());
        assertEquals(new BigDecimal("430"), movement.getTotalCost());
        assertEquals(new BigDecimal("53.750000"), movement.getUnitCost());
        verify(layerRepository).save(layer1);
        verify(layerRepository).save(layer2);
    }

    @Test
    void getValuationReport() {
        TenantSettingDto setting = TenantSettingDto.builder().value("FIFO").build();
        when(tenantSettingService.getSetting("INVENTORY_VALUATION_METHOD")).thenReturn(setting);

        Stock stock = new Stock();
        stock.setProductVariant(productVariant);
        stock.setWarehouse(warehouse);
        stock.setQuantity(new BigDecimal("10"));

        when(stockRepository.findAll(any(Specification.class))).thenReturn(List.of(stock));

        when(layerRepository.calculateTotalValue(productVariant.getId(), warehouse.getId())).thenReturn(new BigDecimal("500"));

        List<InventoryValuationReportDto> report = valuationService.getValuationReport(warehouse.getId());

        assertEquals(1, report.size());
        assertEquals(new BigDecimal("10"), report.get(0).getQuantity());
        assertEquals(new BigDecimal("500"), report.get(0).getTotalValue());
        assertEquals(new BigDecimal("50.000000"), report.get(0).getUnitCost());
    }
}
