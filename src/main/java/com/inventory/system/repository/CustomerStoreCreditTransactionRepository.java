package com.inventory.system.repository;

import com.inventory.system.common.entity.CustomerStoreCreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerStoreCreditTransactionRepository extends JpaRepository<CustomerStoreCreditTransaction, UUID> {
    Page<CustomerStoreCreditTransaction> findByCustomerId(UUID customerId, Pageable pageable);
}