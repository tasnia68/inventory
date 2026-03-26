package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountsReceivableInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountsReceivableInvoiceRepository extends JpaRepository<AccountsReceivableInvoice, UUID>, JpaSpecificationExecutor<AccountsReceivableInvoice> {
}
