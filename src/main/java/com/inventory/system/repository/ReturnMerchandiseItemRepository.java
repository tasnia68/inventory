package com.inventory.system.repository;

import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import com.inventory.system.common.entity.ReturnMerchandiseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface ReturnMerchandiseItemRepository extends JpaRepository<ReturnMerchandiseItem, UUID> {

    @Query("""
	    SELECT COALESCE(SUM(ri.quantity), 0)
	    FROM ReturnMerchandiseItem ri
	    WHERE ri.salesOrderItem.id = :salesOrderItemId
	      AND ri.rma.status IN :statuses
	    """)
    BigDecimal sumQuantityBySalesOrderItemIdAndRmaStatusIn(
	    @Param("salesOrderItemId") UUID salesOrderItemId,
	    @Param("statuses") Collection<ReturnMerchandiseStatus> statuses);

    @Query("""
	    SELECT COALESCE(SUM(ri.quantity), 0)
	    FROM ReturnMerchandiseItem ri
	    WHERE ri.salesOrderItem.id = :salesOrderItemId
	      AND ri.rma.shipment.id = :shipmentId
	      AND ri.rma.status IN :statuses
	    """)
    BigDecimal sumQuantityBySalesOrderItemIdAndShipmentIdAndRmaStatusIn(
	    @Param("salesOrderItemId") UUID salesOrderItemId,
	    @Param("shipmentId") UUID shipmentId,
	    @Param("statuses") Collection<ReturnMerchandiseStatus> statuses);
}