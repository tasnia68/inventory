package com.inventory.system.repository;

import com.inventory.system.common.entity.SalesRefundAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalesRefundAuditEntryRepository extends JpaRepository<SalesRefundAuditEntry, UUID> {
}