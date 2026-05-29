package com.inventory.system.repository;

import com.inventory.system.common.entity.DiscountRedemption;
import com.inventory.system.common.entity.DiscountRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, UUID> {

    void deleteBySalesOrderId(UUID salesOrderId);

    void deleteByPosSaleId(UUID posSaleId);

    long countByDiscountIdAndStatus(UUID discountId, DiscountRedemptionStatus status);

    long countByDiscountIdAndCustomerIdAndStatus(UUID discountId, UUID customerId, DiscountRedemptionStatus status);

    long countByDiscountCodeIdAndStatus(UUID discountCodeId, DiscountRedemptionStatus status);

    long countByDiscountCodeIdAndCustomerIdAndStatus(UUID discountCodeId, UUID customerId, DiscountRedemptionStatus status);

    long countByDiscountCodeIdAndCustomerIdAndRedeemedAtAfter(UUID discountCodeId, UUID customerId, LocalDateTime redeemedAfter);

    long countByCustomerIdAndStatus(UUID customerId, DiscountRedemptionStatus status);

    @Query("""
            select coalesce(sum(r.discountAmount), 0)
            from DiscountRedemption r
            where r.redeemedAt between :from and :to
              and r.status = com.inventory.system.common.entity.DiscountRedemptionStatus.APPLIED
            """)
    BigDecimal sumDiscountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<DiscountRedemption> findByRedeemedAtBetween(LocalDateTime from, LocalDateTime to);

    List<DiscountRedemption> findByDiscountId(UUID discountId);
}
