package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.TenantDatasourceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Phase 5 — orchestrates a shared→dedicated cutover for one tenant.
 *
 * <p>Sequence: validate → {@code MIGRATING} (the tenant fails closed for the
 * window, so no writes land in the shared DB mid-copy) → migrate the dedicated
 * schema to baseline → app-level filtered row copy → verify every table's
 * source/copied counts match → atomic flip to {@code DEDICATED/ACTIVE} →
 * invalidate catalog cache + evict any stale pool.
 *
 * <p>On any failure the tenant is left non-ACTIVE with {@code last_error}
 * (rolls back to failing closed); the shared data is never mutated, so the
 * tenant simply stays on the shared DB until retried.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class TenantCutoverService {

    private static final Logger log = LoggerFactory.getLogger(TenantCutoverService.class);

    private final JdbcTemplate jdbc;
    private final DataSource sharedDataSource;
    private final CredentialCipher cipher;
    private final DedicatedFlywayMigrator migrator;
    private final TenantDataCopier copier;
    private final TenantCatalogService catalog;
    private final TenantDataSourceRegistry registry;

    public TenantCutoverService(DataSource sharedDataSource,
                                CredentialCipher cipher,
                                DedicatedFlywayMigrator migrator,
                                TenantDataCopier copier,
                                TenantCatalogService catalog,
                                TenantDataSourceRegistry registry) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.sharedDataSource = sharedDataSource;
        this.cipher = cipher;
        this.migrator = migrator;
        this.copier = copier;
        this.catalog = catalog;
        this.registry = registry;
    }

    public record CutoverResult(String tenantId, String schemaVersion,
                                Map<String, TenantDataCopier.TableCount> tableReport) {}

    public CutoverResult cutover(String tenantId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT mode, jdbc_url_enc, jdbc_username_enc, jdbc_password_enc "
                + "FROM tenant_datasource WHERE tenant_id = ?", tenantId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No tenant_datasource row for tenant " + tenantId);
        }
        Map<String, Object> row = rows.get(0);
        if (TenantDatasourceMode.valueOf(String.valueOf(row.get("mode"))) != TenantDatasourceMode.DEDICATED) {
            throw new IllegalStateException("Cutover requires mode=DEDICATED for tenant " + tenantId);
        }

        String url = decrypt(row.get("jdbc_url_enc"), tenantId, "jdbc url");
        String user = decrypt(row.get("jdbc_username_enc"), tenantId, "username");
        String pass = decrypt(row.get("jdbc_password_enc"), tenantId, "password");

        String sharedUrl = sharedJdbcUrl();
        if (sharedUrl != null && normalize(sharedUrl).equals(normalize(url))) {
            throw new IllegalStateException(
                    "Refusing cutover for tenant " + tenantId + ": target URL equals the shared DB URL");
        }

        setStatus(tenantId, "MIGRATING", null);
        try {
            String version = migrator.migrateToBaseline(url, user, pass);

            Map<String, TenantDataCopier.TableCount> reportRaw =
                    copier.copyTenant(tenantId, url, user, pass);

            // Verify gate — refuse to flip unless every table copied exactly.
            List<String> mismatches = reportRaw.entrySet().stream()
                    .filter(e -> !e.getValue().matches())
                    .map(e -> e.getKey() + " (src=" + e.getValue().source()
                            + ", copied=" + e.getValue().copied() + ")")
                    .toList();
            if (!mismatches.isEmpty()) {
                throw new IllegalStateException(
                        "Row-count verification failed for tenant " + tenantId + ": " + mismatches);
            }

            // Atomic flip: a single UPDATE makes the tenant DEDICATED+ACTIVE.
            jdbc.update("UPDATE tenant_datasource SET status='ACTIVE', flyway_version=?, "
                    + "provisioned_at=COALESCE(provisioned_at,?), last_migrated_at=?, "
                    + "last_error=NULL, updated_at=? WHERE tenant_id=?",
                    version, ts(), ts(), ts(), tenantId);
            catalog.invalidate(tenantId);
            registry.evict(tenantId); // drop any stale binding; rebuilt lazily
            log.info("Cutover complete for tenant {} (schema {})", tenantId, version);
            return new CutoverResult(tenantId, version, reportRaw);
        } catch (RuntimeException e) {
            // Fail safe: tenant stays non-ACTIVE (fails closed); shared data
            // untouched, so it simply remains on shared until retried.
            setStatus(tenantId, "PENDING", truncate(e.getMessage(), 4000));
            log.error("Cutover failed for tenant {} (left on shared): {}", tenantId, e.getMessage());
            throw e;
        }
    }

    private void setStatus(String tenantId, String status, String error) {
        jdbc.update("UPDATE tenant_datasource SET status=?, last_error=?, updated_at=? WHERE tenant_id=?",
                status, error, ts(), tenantId);
        catalog.invalidate(tenantId);
    }

    private String decrypt(Object enc, String tenantId, String what) {
        String s = enc == null ? null : String.valueOf(enc);
        if (!StringUtils.hasText(s)) {
            throw new IllegalStateException("Tenant " + tenantId + " is DEDICATED but " + what + " is not set");
        }
        return cipher.decrypt(s);
    }

    private String sharedJdbcUrl() {
        try (Connection c = sharedDataSource.getConnection()) {
            return c.getMetaData().getURL();
        } catch (Exception e) {
            log.warn("Could not read shared DB URL for safety comparison: {}", e.getMessage());
            return null;
        }
    }

    private static String normalize(String url) {
        if (url == null) return "";
        int cut = url.length();
        int q = url.indexOf('?');
        int s = url.indexOf(';');
        if (q >= 0) cut = Math.min(cut, q);
        if (s >= 0) cut = Math.min(cut, s);
        return url.substring(0, cut).trim().toLowerCase();
    }

    private static Timestamp ts() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private static String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
