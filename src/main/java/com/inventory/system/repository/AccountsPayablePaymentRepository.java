package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountsPayablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountsPayablePaymentRepository extends JpaRepository<AccountsPayablePayment, UUID>, JpaSpecificationExecutor<AccountsPayablePayment> {
}
