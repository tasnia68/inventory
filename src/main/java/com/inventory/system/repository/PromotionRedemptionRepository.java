package com.inventory.system.repository;

import com.inventory.system.common.entity.PromotionRedemption;
import com.inventory.system.common.entity.PromotionRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, UUID> {

  void deleteBySalesOrderId(UUID salesOrderId);

  void deleteByPosSaleId(UUID posSaleId);

    long countByPromotionIdAndStatus(UUID promotionId, PromotionRedemptionStatus status);

    long countByPromotionIdAndCustomerIdAndStatus(UUID promotionId, UUID customerId, PromotionRedemptionStatus status);

    long countByCouponIdAndStatus(UUID couponId, PromotionRedemptionStatus status);

    long countByCouponIdAndCustomerIdAndStatus(UUID couponId, UUID customerId, PromotionRedemptionStatus status);

    long countByCouponIdAndCustomerIdAndRedeemedAtAfter(UUID couponId, UUID customerId, LocalDateTime redeemedAfter);

    @Query("""
            select coalesce(sum(r.discountAmount), 0)
            from PromotionRedemption r
            where r.redeemedAt between :from and :to
              and r.status = com.inventory.system.common.entity.PromotionRedemptionStatus.APPLIED
            """)
    BigDecimal sumDiscountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<PromotionRedemption> findByRedeemedAtBetween(LocalDateTime from, LocalDateTime to);
}