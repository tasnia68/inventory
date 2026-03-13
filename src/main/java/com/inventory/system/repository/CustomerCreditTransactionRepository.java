package com.inventory.system.repository;

import com.inventory.system.common.entity.CustomerCreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerCreditTransactionRepository extends JpaRepository<CustomerCreditTransaction, UUID> {
    Page<CustomerCreditTransaction> findByCustomerId(UUID customerId, Pageable pageable);
}