package com.inventory.system.repository;

import com.inventory.system.common.entity.GiftCardTransaction;
import com.inventory.system.common.entity.GiftCardTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GiftCardTransactionRepository extends JpaRepository<GiftCardTransaction, UUID> {
    List<GiftCardTransaction> findByGiftCardIdOrderByOccurredAtDesc(UUID giftCardId);

    List<GiftCardTransaction> findBySalesOrderIdAndType(UUID salesOrderId, GiftCardTransactionType type);
}
