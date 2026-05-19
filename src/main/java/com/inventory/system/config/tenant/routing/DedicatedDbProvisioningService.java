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
 * Phase 4 — provisions / (re-)migrates a tenant's dedicated database to the
 * application schema baseline.
 *
 * <p>Control-plane via plain JDBC on the shared DataSource (same reasoning as
 * {@link TenantCatalogService}). Admin-invoked post-startup, so no EMF cycle.
 *
 * <p><strong>Guards (mitigation L10):</strong> refuses to migrate unless the
 * catalog row exists and is {@code mode=DEDICATED}, and the target JDBC URL is
 * not the shared database's URL — so a misconfiguration can never run tenant
 * migrations against the shared DB.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class DedicatedDbProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(DedicatedDbProvisioningService.class);

    private final JdbcTemplate jdbc;
    private final DataSource sharedDataSource;
    private final CredentialCipher cipher;
    private final DedicatedFlywayMigrator migrator;
    private final TenantCatalogService catalog;

    public DedicatedDbProvisioningService(DataSource sharedDataSource,
                                          CredentialCipher cipher,
                                          DedicatedFlywayMigrator migrator,
                                          TenantCatalogService catalog) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.sharedDataSource = sharedDataSource;
        this.cipher = cipher;
        this.migrator = migrator;
        this.catalog = catalog;
    }

    /**
     * Migrate (or provision) the tenant's dedicated DB to baseline and mark it
     * ACTIVE. Returns the resulting schema version.
     */
    public String migrateTenant(String tenantId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT mode, jdbc_url_enc, jdbc_username_enc, jdbc_password_enc "
                + "FROM tenant_datasource WHERE tenant_id = ?", tenantId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No tenant_datasource row for tenant " + tenantId);
        }
        Map<String, Object> row = rows.get(0);

        TenantDatasourceMode mode =
                TenantDatasourceMode.valueOf(String.valueOf(row.get("mode")));
        if (mode != TenantDatasourceMode.DEDICATED) {
            throw new IllegalStateException(
                    "Refusing to migrate tenant " + tenantId + ": mode is " + mode + ", not DEDICATED");
        }

        String url = decrypt(row.get("jdbc_url_enc"), tenantId, "jdbc url");
        String user = decrypt(row.get("jdbc_username_enc"), tenantId, "username");
        String pass = decrypt(row.get("jdbc_password_enc"), tenantId, "password");

        String sharedUrl = sharedJdbcUrl();
        if (sharedUrl != null && normalize(sharedUrl).equals(normalize(url))) {
            throw new IllegalStateException(
                    "Refusing to migrate tenant " + tenantId
                    + ": target URL equals the shared database URL");
        }

        setStatus(tenantId, "MIGRATING", null);
        try {
            String version = migrator.migrateToBaseline(url, user, pass);
            jdbc.update("UPDATE tenant_datasource SET status='ACTIVE', flyway_version=?, "
                    + "last_migrated_at=?, provisioned_at=COALESCE(provisioned_at, ?), "
                    + "last_error=NULL, updated_at=? WHERE tenant_id=?",
                    version, ts(), ts(), ts(), tenantId);
            catalog.invalidate(tenantId);
            log.info("Migrated dedicated DB for tenant {} to schema {}", tenantId, version);
            return version;
        } catch (RuntimeException e) {
            // Fail safe: leave the tenant NON-active so it fails closed, record why.
            setStatus(tenantId, "PENDING", truncate(e.getMessage(), 4000));
            catalog.invalidate(tenantId);
            log.error("Dedicated DB migration failed for tenant {}: {}", tenantId, e.getMessage());
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
            throw new IllegalStateException(
                    "Tenant " + tenantId + " is DEDICATED but " + what + " is not set");
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
        // Drop connection parameters: Postgres uses '?', H2/others use ';'.
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
