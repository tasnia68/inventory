package com.inventory.system.service;

import com.inventory.system.payload.ReplenishmentRuleDto;
import com.inventory.system.payload.ReplenishmentSuggestionDto;
import java.util.List;
import java.util.UUID;

public interface ReplenishmentService {
    ReplenishmentRuleDto createRule(ReplenishmentRuleDto dto);
    ReplenishmentRuleDto updateRule(UUID id, ReplenishmentRuleDto dto);
    void deleteRule(UUID id);
    ReplenishmentRuleDto getRule(UUID id);
    List<ReplenishmentRuleDto> getRulesByWarehouse(UUID warehouseId);

    void calculateReorderPoint(UUID ruleId);
    List<ReplenishmentSuggestionDto> getReplenishmentSuggestions(UUID warehouseId);
}
