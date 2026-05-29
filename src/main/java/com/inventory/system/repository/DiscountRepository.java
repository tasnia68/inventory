package com.inventory.system.repository;

import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.DiscountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    List<Discount> findByStatus(DiscountStatus status);

    List<Discount> findByAutoApplyTrueAndStatus(DiscountStatus status);
}
