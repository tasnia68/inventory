package com.inventory.system.repository;

import com.inventory.system.common.entity.SupplierReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, UUID> {
    List<SupplierReturn> findByGoodsReceiptNoteIdOrderByCreatedAtDesc(UUID goodsReceiptNoteId);
}