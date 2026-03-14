package com.inventory.system.repository;

import com.inventory.system.common.entity.PickingStatus;
import com.inventory.system.common.entity.PickingTask;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface PickingTaskRepository extends JpaRepository<PickingTask, UUID>, JpaSpecificationExecutor<PickingTask> {

    @Query("""
	    SELECT COALESCE(SUM(t.requestedQuantity), 0)
	    FROM PickingTask t
	    WHERE t.salesOrderItem.id = :salesOrderItemId
	      AND t.pickingList.status <> :cancelledStatus
	    """)
    BigDecimal sumRequestedQuantityBySalesOrderItemIdExcludingListStatus(
	    @Param("salesOrderItemId") UUID salesOrderItemId,
	    @Param("cancelledStatus") PickingStatus cancelledStatus);

    @Query("""
	    SELECT COALESCE(SUM(t.pickedQuantity), 0)
	    FROM PickingTask t
	    WHERE t.salesOrderItem.id = :salesOrderItemId
	      AND t.pickingList.status = :completedStatus
	      AND t.status = :completedStatus
	    """)
    BigDecimal sumPickedQuantityBySalesOrderItemIdForCompletedLists(
	    @Param("salesOrderItemId") UUID salesOrderItemId,
	    @Param("completedStatus") PickingStatus completedStatus);
}
