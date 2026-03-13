package com.inventory.system.repository;

import com.inventory.system.common.entity.PurchaseRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, UUID> {
    List<PurchaseRequisition> findByWarehouseId(UUID warehouseId);
}
