package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.TenantDatasource;
import com.inventory.system.common.entity.TenantDatasourceMode;
import com.inventory.system.common.entity.TenantDatasourceStatus;
import com.inventory.system.repository.TenantDatasourceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves, per tenant, whether persistence routes to the shared pool or a
 * dedicated database. The {@code tenant_datasource} catalog is the source of
 * truth; results are cached with a short TTL and explicitly invalidated on
 * mutation.
 *
 * <p>Safe-by-default: a missing catalog row resolves to SHARED. Any
 * non-ACTIVE status, or a DEDICATED row with unusable credentials, throws
 * {@link TenantDatasourceUnavailableException} (fail-closed) — it must never
 * degrade to the shared DB.
 *
 * <p>Gated by {@code app.tenant.routing.enabled}; absent as a bean (and thus
 * inert) until Phase 2 sign-off.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class TenantCatalogService {

    private record CacheEntry(ResolvedRouting routing, long expiresAtMillis) {}

    private final TenantDatasourceRepository repository;
    private final CredentialCipher cipher;
    private final TenantRoutingProperties props;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TenantCatalogService(TenantDatasourceRepository repository,
                                CredentialCipher cipher,
                                TenantRoutingProperties props) {
        this.repository = repository;
        this.cipher = cipher;
        this.props = props;
    }

    public void invalidate(String tenantId) {
        if (tenantId != null) cache.remove(tenantId);
    }

    public void invalidateAll() {
        cache.clear();
    }

    @Transactional(readOnly = true)
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
        TenantDatasource row = repository.findById(tenantId).orElse(null);

        // Absence = shared/active default (a missing row can never be misread
        // as dedicated).
        if (row == null) {
            return ResolvedRouting.shared(sharedKey);
        }

        // Any non-ACTIVE status fails closed regardless of mode (operator
        // disabled, provisioning, or mid-cutover).
        if (row.getStatus() != TenantDatasourceStatus.ACTIVE) {
            throw new TenantDatasourceUnavailableException(
                    "Tenant " + tenantId + " datasource is " + row.getStatus()
                    + " (not ACTIVE) — failing closed");
        }

        if (row.getMode() == TenantDatasourceMode.SHARED) {
            return ResolvedRouting.shared(sharedKey);
        }

        // DEDICATED + ACTIVE: decrypt connection details in-memory.
        String url = decryptRequired(row.getJdbcUrlEnc(), tenantId, "jdbc url");
        String user = decryptRequired(row.getJdbcUsernameEnc(), tenantId, "username");
        String pass = decryptRequired(row.getJdbcPasswordEnc(), tenantId, "password");
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
