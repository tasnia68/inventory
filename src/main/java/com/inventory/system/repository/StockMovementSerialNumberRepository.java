package com.inventory.system.repository;

import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockMovementSerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementSerialNumberRepository extends JpaRepository<StockMovementSerialNumber, UUID> {
    List<StockMovementSerialNumber> findByStockMovement(StockMovement stockMovement);
    List<StockMovementSerialNumber> findBySerialNumber(SerialNumber serialNumber);
}
