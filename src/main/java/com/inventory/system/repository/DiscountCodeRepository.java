package com.inventory.system.repository;

import com.inventory.system.common.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    List<DiscountCode> findByDiscountId(UUID discountId);
}
