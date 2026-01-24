package com.inventory.system.repository;

import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {
    boolean existsByShipmentNumber(String shipmentNumber);
    List<Shipment> findBySalesOrderId(UUID salesOrderId);
    List<Shipment> findByStatus(ShipmentStatus status);
}
