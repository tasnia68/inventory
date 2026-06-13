package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountingAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountingAuditLogRepository extends JpaRepository<AccountingAuditLog, UUID> {
    List<AccountingAuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
