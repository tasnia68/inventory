package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.TenantDatasourceMode;
import com.inventory.system.common.entity.TenantDatasourceStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves, per tenant, whether persistence routes to the shared pool or a
 * dedicated database. The {@code tenant_datasource} catalog is the source of
 * truth; results are cached with a short TTL and explicitly invalidated on
 * mutation.
 *
 * <p><strong>Reads the catalog via plain JDBC on the shared DataSource — never
 * via JPA.</strong> The connection provider is consumed while Hibernate's
 * EntityManagerFactory is being built, so any dependency on a JPA repository
 * here would create an unresolvable circular reference (EMF → customizer →
 * provider → catalog → repository → EMF). The control-plane must be readable
 * independently of the persistence unit it configures.
 *
 * <p>Safe-by-default: a missing catalog row resolves to SHARED. Any
 * non-ACTIVE status, or a DEDICATED row with unusable credentials, throws
 * {@link TenantDatasourceUnavailableException} (fail-closed) — it must never
 * degrade to the shared DB.
 *
 * <p>Gated by {@code app.tenant.routing.enabled}; absent (and inert) until
 * Phase 2 sign-off.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class TenantCatalogService {

    private static final String SQL =
            "SELECT mode, status, jdbc_url_enc, jdbc_username_enc, jdbc_password_enc, flyway_version "
            + "FROM tenant_datasource WHERE tenant_id = ?";

    private record CacheEntry(ResolvedRouting routing, long expiresAtMillis) {}

    private final JdbcTemplate jdbc;
    private final CredentialCipher cipher;
    private final TenantRoutingProperties props;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TenantCatalogService(DataSource sharedDataSource,
                                CredentialCipher cipher,
                                TenantRoutingProperties props) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.cipher = cipher;
        this.props = props;
    }

    public void invalidate(String tenantId) {
        if (tenantId != null) cache.remove(tenantId);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public ResolvedRouting resolve(String tenantId) {
        String sharedKey = props.getSharedIdentifier();
        if (!StringUtils.hasText(tenantId) || sharedKey.equals(tenantId)) {
            return ResolvedRouting.shared(sharedKey);
        }

        CacheEntry cached = cache.get(tenantId);
        if (cached != null && cached.expiresAtMillis() > System.currentTimeMillis()) {
            return cached.routing();
        }

        ResolvedRouting resolved = resolveUncached(tenantId, sharedKey);
        cache.put(tenantId, new CacheEntry(resolved,
                System.currentTimeMillis() + props.getCatalogCacheTtlSeconds() * 1000L));
        return resolved;
    }

    private ResolvedRouting resolveUncached(String tenantId, String sharedKey) {
        List<Map<String, Object>> rows = jdbc.queryForList(SQL, tenantId);

        // Absence = shared/active default (a missing row can never be misread
        // as dedicated).
        if (rows.isEmpty()) {
            return ResolvedRouting.shared(sharedKey);
        }

        Map<String, Object> row = rows.get(0);
        TenantDatasourceStatus status =
                TenantDatasourceStatus.valueOf(String.valueOf(row.get("status")));
        TenantDatasourceMode mode =
                TenantDatasourceMode.valueOf(String.valueOf(row.get("mode")));

        // Any non-ACTIVE status fails closed regardless of mode.
        if (status != TenantDatasourceStatus.ACTIVE) {
            throw new TenantDatasourceUnavailableException(
                    "Tenant " + tenantId + " datasource is " + status
                    + " (not ACTIVE) — failing closed");
        }

        if (mode == TenantDatasourceMode.SHARED) {
            return ResolvedRouting.shared(sharedKey);
        }

        // Schema-baseline gate: a dedicated DB whose migrations are behind the
        // application baseline must not be served (fail closed). Disabled while
        // expectedSchemaVersion is blank (Phase 4 provisioning turns it on).
        String expected = props.getExpectedSchemaVersion();
        if (StringUtils.hasText(expected)) {
            String actual = row.get("flyway_version") == null
                    ? null : String.valueOf(row.get("flyway_version"));
            if (!expected.equals(actual)) {
                throw new TenantDatasourceUnavailableException(
                        "Tenant " + tenantId + " dedicated DB schema is '" + actual
                        + "' but application baseline is '" + expected + "' — failing closed");
            }
        }

        // DEDICATED + ACTIVE: decrypt connection details in-memory.
        String url = decryptRequired((String) row.get("jdbc_url_enc"), tenantId, "jdbc url");
        String user = decryptRequired((String) row.get("jdbc_username_enc"), tenantId, "username");
        String pass = decryptRequired((String) row.get("jdbc_password_enc"), tenantId, "password");
        return ResolvedRouting.dedicated(tenantId, url, user, pass, hostOf(url));
    }

    private String decryptRequired(String enc, String tenantId, String what) {
        if (!StringUtils.hasText(enc)) {
            throw new TenantDatasourceUnavailableException(
                    "Tenant " + tenantId + " is DEDICATED but " + what + " is not set — failing closed");
        }
        try {
            String v = cipher.decrypt(enc);
            if (!StringUtils.hasText(v)) {
                throw new IllegalStateException("decrypted " + what + " is empty");
            }
            return v;
        } catch (RuntimeException e) {
            throw new TenantDatasourceUnavailableException(
                    "Tenant " + tenantId + " " + what + " could not be decrypted — failing closed", e);
        }
    }

    /** Extract host (no creds) from a jdbc url for audit logging. */
    static String hostOf(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        int slashes = jdbcUrl.indexOf("//");
        if (slashes < 0) return null;
        String rest = jdbcUrl.substring(slashes + 2);
        int end = rest.indexOf('/');
        String authority = end < 0 ? rest : rest.substring(0, end);
        int at = authority.indexOf('@');
        return at < 0 ? authority : authority.substring(at + 1);
    }
}
