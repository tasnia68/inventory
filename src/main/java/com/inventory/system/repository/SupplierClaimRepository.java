package com.inventory.system.repository;

import com.inventory.system.common.entity.SupplierClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierClaimRepository extends JpaRepository<SupplierClaim, UUID> {
    List<SupplierClaim> findByGoodsReceiptNoteIdOrderByCreatedAtDesc(UUID goodsReceiptNoteId);
    Optional<SupplierClaim> findFirstByDamageRecordIdOrderByCreatedAtDesc(UUID damageRecordId);
}