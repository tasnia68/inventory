package com.inventory.system.repository;

import com.inventory.system.common.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID>, JpaSpecificationExecutor<StockTransaction> {
    Optional<StockTransaction> findByTransactionNumber(String transactionNumber);
}
