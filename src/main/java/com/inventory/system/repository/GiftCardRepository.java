package com.inventory.system.repository;

import com.inventory.system.common.entity.GiftCard;
import com.inventory.system.common.entity.GiftCardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GiftCardRepository extends JpaRepository<GiftCard, UUID> {
    Optional<GiftCard> findByCodeIgnoreCase(String code);

    List<GiftCard> findByStatus(GiftCardStatus status);

    List<GiftCard> findByIssuedToCustomerId(UUID customerId);
}
