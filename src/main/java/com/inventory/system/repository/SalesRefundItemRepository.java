package com.inventory.system.repository;

import com.inventory.system.common.entity.SalesRefundItem;
import com.inventory.system.common.entity.SalesRefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public interface SalesRefundItemRepository extends JpaRepository<SalesRefundItem, UUID> {

    @Query("""
            select coalesce(sum(i.quantity), 0)
            from SalesRefundItem i
            where i.salesOrderItem.id = :salesOrderItemId
              and i.salesRefund.status in :statuses
            """)
    BigDecimal sumQuantityBySalesOrderItemIdAndRefundStatusIn(@Param("salesOrderItemId") UUID salesOrderItemId,
                                                              @Param("statuses") Collection<SalesRefundStatus> statuses);
}