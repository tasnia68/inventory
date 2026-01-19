package com.inventory.system.repository;

import com.inventory.system.common.entity.ReplenishmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReplenishmentRuleRepository extends JpaRepository<ReplenishmentRule, UUID>, JpaSpecificationExecutor<ReplenishmentRule> {

    List<ReplenishmentRule> findByWarehouseId(UUID warehouseId);

    Optional<ReplenishmentRule> findByWarehouseIdAndProductVariantId(UUID warehouseId, UUID productVariantId);

    List<ReplenishmentRule> findByWarehouseIdAndIsEnabledTrue(UUID warehouseId);
}
