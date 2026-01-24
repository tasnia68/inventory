package com.inventory.system.repository;

import com.inventory.system.common.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {
    boolean existsBySoNumber(String soNumber);
    Optional<SalesOrder> findBySoNumber(String soNumber);
}
