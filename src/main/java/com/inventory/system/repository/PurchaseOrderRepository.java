package com.inventory.system.repository;

import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    Page<PurchaseOrder> findBySupplierId(UUID supplierId, Pageable pageable);

    // Additional queries can be added as needed for filtering by status AND supplier, etc.
    Page<PurchaseOrder> findBySupplierIdAndStatus(UUID supplierId, PurchaseOrderStatus status, Pageable pageable);
}
