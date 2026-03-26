package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountsReceivablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountsReceivablePaymentRepository extends JpaRepository<AccountsReceivablePayment, UUID>, JpaSpecificationExecutor<AccountsReceivablePayment> {
}
