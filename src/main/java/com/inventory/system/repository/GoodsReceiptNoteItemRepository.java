package com.inventory.system.repository;

import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoodsReceiptNoteItemRepository extends JpaRepository<GoodsReceiptNoteItem, UUID> {
}
