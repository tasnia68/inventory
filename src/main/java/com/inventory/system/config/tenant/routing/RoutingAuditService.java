package com.inventory.system.config.tenant.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists routing-decision audit rows.
 *
 * <p>Uses plain JDBC on the shared DataSource (not JPA) for the same reason as
 * {@link TenantCatalogService}: this is on the connection-provider path that is
 * wired while the JPA EntityManagerFactory is being built, so it must not
 * depend on the persistence unit. Best-effort: an audit failure is logged and
 * swallowed and never affects request flow — but a fail-closed denial is still
 * thrown by the caller regardless of whether the audit row was written.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class RoutingAuditService {

    private static final Logger log = LoggerFactory.getLogger(RoutingAuditService.class);

    private static final String INSERT =
            "INSERT INTO tenant_routing_audit "
            + "(id, tenant_id, decision, reason, jdbc_url_host, principal, request_path, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;

    public RoutingAuditService(DataSource sharedDataSource) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
    }

    public void record(String tenantId, RoutingDecision decision, String reason,
                        String host, String principal, String requestPath) {
        try {
            jdbc.update(INSERT,
                    UUID.randomUUID(),
                    tenantId == null ? "(none)" : tenantId,
                    decision.name(),
                    truncate(reason, 4000),
                    truncate(host, 255),
                    truncate(principal, 255),
                    truncate(requestPath, 512),
                    Timestamp.valueOf(LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Failed to write tenant routing audit ({} {}): {}",
                    tenantId, decision, e.getMessage());
        }
    }

    private static String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
