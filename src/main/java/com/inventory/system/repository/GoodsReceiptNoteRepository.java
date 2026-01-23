package com.inventory.system.repository;

import com.inventory.system.common.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, UUID>, JpaSpecificationExecutor<GoodsReceiptNote> {
    boolean existsByGrnNumber(String grnNumber);
}
