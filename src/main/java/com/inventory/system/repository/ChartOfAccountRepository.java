package com.inventory.system.repository;

import com.inventory.system.common.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID>, JpaSpecificationExecutor<ChartOfAccount> {
    Optional<ChartOfAccount> findByAccountCode(String accountCode);
}
