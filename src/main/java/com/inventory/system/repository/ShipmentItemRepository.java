package com.inventory.system.repository;

import com.inventory.system.common.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, UUID> {

    @Query("""
	    SELECT COALESCE(SUM(si.quantity), 0)
	    FROM ShipmentItem si
	    WHERE si.salesOrderItem.id = :salesOrderItemId
	      AND si.shipment.id = :shipmentId
	    """)
    BigDecimal sumQuantityBySalesOrderItemIdAndShipmentId(
	    @Param("salesOrderItemId") UUID salesOrderItemId,
	    @Param("shipmentId") UUID shipmentId);
}