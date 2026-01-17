package com.inventory.system.repository;

import com.inventory.system.common.entity.StockTransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockTransactionItemRepository extends JpaRepository<StockTransactionItem, UUID> {
}
