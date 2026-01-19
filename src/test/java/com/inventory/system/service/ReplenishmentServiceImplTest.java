package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.payload.ReplenishmentRuleDto;
import com.inventory.system.payload.ReplenishmentSuggestionDto;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.ReplenishmentRuleRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplenishmentServiceImplTest {

    @Mock
    private ReplenishmentRuleRepository replenishmentRuleRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private StockReservationService stockReservationService;

    @InjectMocks
    private ReplenishmentServiceImpl replenishmentService;

    private ReplenishmentRule rule;
    private Warehouse warehouse;
    private ProductVariant productVariant;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Main Warehouse");

        ProductTemplate template = new ProductTemplate();
        template.setName("Test Product");

        productVariant = new ProductVariant();
        productVariant.setId(UUID.randomUUID());
        productVariant.setSku("SKU-123");
        productVariant.setTemplate(template);

        rule = new ReplenishmentRule();
        rule.setId(UUID.randomUUID());
        rule.setWarehouse(warehouse);
        rule.setProductVariant(productVariant);
        rule.setMinStock(new BigDecimal("10.00"));
        rule.setMaxStock(new BigDecimal("100.00"));
        rule.setLeadTimeDays(5);
        rule.setSafetyStock(new BigDecimal("2.00"));
    }

    @Test
    void createRule_ShouldSucceed() {
        ReplenishmentRuleDto dto = new ReplenishmentRuleDto();
        dto.setWarehouseId(warehouse.getId());
        dto.setProductVariantId(productVariant.getId());
        dto.setMinStock(new BigDecimal("10.00"));

        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(productVariantRepository.findById(productVariant.getId())).thenReturn(Optional.of(productVariant));
        when(replenishmentRuleRepository.save(any(ReplenishmentRule.class))).thenReturn(rule);

        ReplenishmentRuleDto result = replenishmentService.createRule(dto);

        assertNotNull(result);
        verify(replenishmentRuleRepository).save(any(ReplenishmentRule.class));
    }

    @Test
    void getReplenishmentSuggestions_ShouldReturnSuggestion_WhenStockIsLow() {
        when(replenishmentRuleRepository.findByWarehouseIdAndIsEnabledTrue(warehouse.getId()))
                .thenReturn(Collections.singletonList(rule));

        // Mock ATP to be less than min stock (e.g., 5 < 10)
        when(stockReservationService.getAvailableToPromise(productVariant.getId(), warehouse.getId()))
                .thenReturn(new BigDecimal("5.00"));

        List<ReplenishmentSuggestionDto> suggestions = replenishmentService.getReplenishmentSuggestions(warehouse.getId());

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals(new BigDecimal("5.00"), suggestions.get(0).getCurrentStock());
        // Suggested quantity should be MaxStock (100) - Current (5) = 95
        assertEquals(new BigDecimal("95.00"), suggestions.get(0).getSuggestedQuantity());
    }

    @Test
    void getReplenishmentSuggestions_ShouldReturnEmpty_WhenStockIsSufficient() {
        when(replenishmentRuleRepository.findByWarehouseIdAndIsEnabledTrue(warehouse.getId()))
                .thenReturn(Collections.singletonList(rule));

        // Mock ATP to be more than min stock (e.g., 15 > 10)
        when(stockReservationService.getAvailableToPromise(productVariant.getId(), warehouse.getId()))
                .thenReturn(new BigDecimal("15.00"));

        List<ReplenishmentSuggestionDto> suggestions = replenishmentService.getReplenishmentSuggestions(warehouse.getId());

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    void calculateReorderPoint_ShouldUpdateRule() {
        when(replenishmentRuleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));

        // Mock new repository method
        when(stockMovementRepository.sumQuantityByProductAndWarehouseAndDateAndType(
                eq(rule.getProductVariant().getId()),
                eq(rule.getWarehouse().getId()),
                any(LocalDateTime.class),
                anyList()
        )).thenReturn(new BigDecimal("30.00"));

        replenishmentService.calculateReorderPoint(rule.getId());

        // Expected Reorder Point = (1 * 5 lead days) + 2 safety stock = 7
        assertEquals(new BigDecimal("7.000000"), rule.getMinStock());
        verify(replenishmentRuleRepository).save(rule);
    }
}
