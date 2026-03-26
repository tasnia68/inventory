package com.inventory.system.repository;

import com.inventory.system.common.entity.TreasuryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, UUID>, JpaSpecificationExecutor<TreasuryAccount> {
    Optional<TreasuryAccount> findByAccountCode(String accountCode);
}
