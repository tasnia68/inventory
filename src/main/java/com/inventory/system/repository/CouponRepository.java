package com.inventory.system.repository;

import com.inventory.system.common.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCode(String code);
    List<Coupon> findByPromotionId(UUID promotionId);
}