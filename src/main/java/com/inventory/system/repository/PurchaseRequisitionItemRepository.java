package com.inventory.system.repository;

import com.inventory.system.common.entity.PurchaseRequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, UUID> {
    List<PurchaseRequisitionItem> findByPurchaseRequisitionId(UUID purchaseRequisitionId);
}
