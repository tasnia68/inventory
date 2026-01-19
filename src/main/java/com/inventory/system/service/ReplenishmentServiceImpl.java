package com.inventory.system.service;

import com.inventory.system.common.entity.ReplenishmentRule;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ReplenishmentRuleDto;
import com.inventory.system.payload.ReplenishmentSuggestionDto;
import com.inventory.system.repository.ReplenishmentRuleRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.WarehouseRepository;
import com.inventory.system.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private final ReplenishmentRuleRepository replenishmentRuleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockReservationService stockReservationService;

    @Override
    @Transactional
    public ReplenishmentRuleDto createRule(ReplenishmentRuleDto dto) {
        ReplenishmentRule rule = new ReplenishmentRule();
        updateRuleFromDto(rule, dto);
        return mapToDto(replenishmentRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public ReplenishmentRuleDto updateRule(UUID id, ReplenishmentRuleDto dto) {
        ReplenishmentRule rule = replenishmentRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReplenishmentRule", "id", id));
        updateRuleFromDto(rule, dto);
        return mapToDto(replenishmentRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        if (!replenishmentRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReplenishmentRule", "id", id);
        }
        replenishmentRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ReplenishmentRuleDto getRule(UUID id) {
        return replenishmentRuleRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("ReplenishmentRule", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplenishmentRuleDto> getRulesByWarehouse(UUID warehouseId) {
        return replenishmentRuleRepository.findByWarehouseId(warehouseId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void calculateReorderPoint(UUID ruleId) {
        ReplenishmentRule rule = replenishmentRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ReplenishmentRule", "id", ruleId));

        // Calculate Average Daily Usage over last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<StockMovement.StockMovementType> types = List.of(StockMovement.StockMovementType.OUT, StockMovement.StockMovementType.TRANSFER_OUT);

        BigDecimal totalUsage = stockMovementRepository.sumQuantityByProductAndWarehouseAndDateAndType(
                rule.getProductVariant().getId(),
                rule.getWarehouse().getId(),
                thirtyDaysAgo,
                types
        );

        if (totalUsage == null) {
            totalUsage = BigDecimal.ZERO;
        }

        BigDecimal avgDailyUsage = totalUsage.divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP);

        Integer leadTime = rule.getLeadTimeDays() != null ? rule.getLeadTimeDays() : 0;
        BigDecimal safetyStock = rule.getSafetyStock() != null ? rule.getSafetyStock() : BigDecimal.ZERO;

        BigDecimal reorderPoint = avgDailyUsage.multiply(BigDecimal.valueOf(leadTime)).add(safetyStock);

        rule.setMinStock(reorderPoint);
        replenishmentRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplenishmentSuggestionDto> getReplenishmentSuggestions(UUID warehouseId) {
        List<ReplenishmentRule> rules = replenishmentRuleRepository.findByWarehouseIdAndIsEnabledTrue(warehouseId);
        List<ReplenishmentSuggestionDto> suggestions = new ArrayList<>();

        for (ReplenishmentRule rule : rules) {
            BigDecimal availableStock = stockReservationService.getAvailableToPromise(
                    rule.getProductVariant().getId(),
                    rule.getWarehouse().getId()
            );

            if (availableStock.compareTo(rule.getMinStock()) <= 0) {
                ReplenishmentSuggestionDto suggestion = new ReplenishmentSuggestionDto();
                suggestion.setProductVariantId(rule.getProductVariant().getId());
                // Combine template name and sku?
                suggestion.setProductVariantName(rule.getProductVariant().getTemplate().getName() + " - " + rule.getProductVariant().getSku());
                suggestion.setSku(rule.getProductVariant().getSku());
                suggestion.setWarehouseId(rule.getWarehouse().getId());
                suggestion.setWarehouseName(rule.getWarehouse().getName());
                suggestion.setCurrentStock(availableStock);
                suggestion.setMinStock(rule.getMinStock());
                suggestion.setMaxStock(rule.getMaxStock());

                BigDecimal quantityNeeded;
                if (rule.getReorderQuantity() != null && rule.getReorderQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    quantityNeeded = rule.getReorderQuantity();
                } else {
                    quantityNeeded = rule.getMaxStock().subtract(availableStock);
                }
                suggestion.setSuggestedQuantity(quantityNeeded.compareTo(BigDecimal.ZERO) > 0 ? quantityNeeded : BigDecimal.ZERO);

                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }

    private void updateRuleFromDto(ReplenishmentRule rule, ReplenishmentRuleDto dto) {
        if (rule.getProductVariant() == null || !rule.getProductVariant().getId().equals(dto.getProductVariantId())) {
             ProductVariant pv = productVariantRepository.findById(dto.getProductVariantId())
                 .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", dto.getProductVariantId()));
             rule.setProductVariant(pv);
        }
        if (rule.getWarehouse() == null || !rule.getWarehouse().getId().equals(dto.getWarehouseId())) {
             Warehouse w = warehouseRepository.findById(dto.getWarehouseId())
                 .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", dto.getWarehouseId()));
             rule.setWarehouse(w);
        }

        rule.setMinStock(dto.getMinStock());
        rule.setMaxStock(dto.getMaxStock());
        rule.setReorderQuantity(dto.getReorderQuantity());
        rule.setSafetyStock(dto.getSafetyStock());
        rule.setLeadTimeDays(dto.getLeadTimeDays());
        rule.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
    }

    private ReplenishmentRuleDto mapToDto(ReplenishmentRule rule) {
        ReplenishmentRuleDto dto = new ReplenishmentRuleDto();
        dto.setId(rule.getId());
        dto.setProductVariantId(rule.getProductVariant().getId());
        dto.setProductVariantName(rule.getProductVariant().getSku());
        dto.setWarehouseId(rule.getWarehouse().getId());
        dto.setWarehouseName(rule.getWarehouse().getName());
        dto.setMinStock(rule.getMinStock());
        dto.setMaxStock(rule.getMaxStock());
        dto.setReorderQuantity(rule.getReorderQuantity());
        dto.setSafetyStock(rule.getSafetyStock());
        dto.setLeadTimeDays(rule.getLeadTimeDays());
        dto.setIsEnabled(rule.getIsEnabled());
        return dto;
    }
}
