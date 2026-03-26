package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountsPayableInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountsPayableInvoiceRepository extends JpaRepository<AccountsPayableInvoice, UUID>, JpaSpecificationExecutor<AccountsPayableInvoice> {
}
