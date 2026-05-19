package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.TenantRoutingAudit;
import com.inventory.system.repository.TenantRoutingAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists routing-decision audit rows. Best-effort and in its own
 * transaction ({@code REQUIRES_NEW}) so an audit failure can never poison or
 * roll back the caller — but a failed audit also never suppresses a
 * fail-closed denial (the caller throws regardless).
 */
@Service
public class RoutingAuditService {

    private static final Logger log = LoggerFactory.getLogger(RoutingAuditService.class);

    private final TenantRoutingAuditRepository repository;

    public RoutingAuditService(TenantRoutingAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tenantId, RoutingDecision decision, String reason,
                        String host, String principal, String requestPath) {
        try {
            TenantRoutingAudit row = new TenantRoutingAudit();
            row.setId(UUID.randomUUID());
            row.setTenantId(tenantId == null ? "(none)" : tenantId);
            row.setDecision(decision.name());
            row.setReason(truncate(reason, 4000));
            row.setJdbcUrlHost(truncate(host, 255));
            row.setPrincipal(truncate(principal, 255));
            row.setRequestPath(truncate(requestPath, 512));
            row.setCreatedAt(LocalDateTime.now());
            repository.save(row);
        } catch (Exception e) {
            // Never let auditing break request flow.
            log.warn("Failed to write tenant routing audit ({} {}): {}",
                    tenantId, decision, e.getMessage());
        }
    }

    private static String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
