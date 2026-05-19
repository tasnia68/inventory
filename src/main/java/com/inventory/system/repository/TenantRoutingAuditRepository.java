package com.inventory.system.repository;

import com.inventory.system.common.entity.TenantRoutingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Control-plane, append-only. Not tenant scoped. */
@Repository
public interface TenantRoutingAuditRepository extends JpaRepository<TenantRoutingAudit, UUID> {
}
