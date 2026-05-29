package com.inventory.system.config.tenant.routing;

import com.inventory.system.payload.TenantDatasourceRequest;
import com.inventory.system.payload.TenantDatasourceResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Super-admin CRUD for the tenant datasource catalog. Control-plane via JDBC
 * on the shared DataSource. Credentials are encrypted on write and never
 * returned on read.
 */
@Service
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class TenantDatasourceAdminService {

    private final JdbcTemplate jdbc;
    private final CredentialCipher cipher;
    private final TenantCatalogService catalog;

    public TenantDatasourceAdminService(DataSource sharedDataSource,
                                        CredentialCipher cipher,
                                        TenantCatalogService catalog) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.cipher = cipher;
        this.catalog = catalog;
    }

    public TenantDatasourceResponse get(String tenantId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT mode,status,flyway_version,jdbc_url_enc,jdbc_password_enc,last_error "
                + "FROM tenant_datasource WHERE tenant_id=?", tenantId);
        if (rows.isEmpty()) {
            // No row = implicit SHARED/ACTIVE default.
            return new TenantDatasourceResponse(tenantId, "SHARED", "ACTIVE", null, null, false, null);
        }
        Map<String, Object> r = rows.get(0);
        String urlEnc = (String) r.get("jdbc_url_enc");
        String host = null;
        if (StringUtils.hasText(urlEnc)) {
            try { host = TenantCatalogService.hostOf(cipher.decrypt(urlEnc)); }
            catch (RuntimeException ignored) { host = "(unreadable)"; }
        }
        return new TenantDatasourceResponse(
                tenantId,
                String.valueOf(r.get("mode")),
                String.valueOf(r.get("status")),
                (String) r.get("flyway_version"),
                host,
                StringUtils.hasText((String) r.get("jdbc_password_enc")),
                (String) r.get("last_error"));
    }

    /** Upsert. DEDICATED rows start as PENDING (provision/cutover activates). */
    public TenantDatasourceResponse upsert(String tenantId, TenantDatasourceRequest req) {
        String mode = "DEDICATED".equalsIgnoreCase(req.getMode()) ? "DEDICATED" : "SHARED";
        boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*)>0 FROM tenant_datasource WHERE tenant_id=?", Boolean.class, tenantId));

        if ("SHARED".equals(mode)) {
            // Reverting to shared clears credentials and is immediately ACTIVE.
            if (exists) {
                jdbc.update("UPDATE tenant_datasource SET mode='SHARED',status='ACTIVE',"
                        + "jdbc_url_enc=NULL,jdbc_username_enc=NULL,jdbc_password_enc=NULL,"
                        + "last_error=NULL,updated_at=? WHERE tenant_id=?", ts(), tenantId);
            } else {
                jdbc.update("INSERT INTO tenant_datasource (tenant_id,mode,status,created_at) "
                        + "VALUES (?, 'SHARED','ACTIVE',?)", tenantId, ts());
            }
            catalog.invalidate(tenantId);
            return get(tenantId);
        }

        String urlEnc = StringUtils.hasText(req.getJdbcUrl()) ? cipher.encrypt(req.getJdbcUrl()) : null;
        String userEnc = StringUtils.hasText(req.getUsername()) ? cipher.encrypt(req.getUsername()) : null;
        String passEnc = StringUtils.hasText(req.getPassword()) ? cipher.encrypt(req.getPassword()) : null;

        if (exists) {
            // Keep existing creds if a field was left blank (write-only edit).
            jdbc.update("UPDATE tenant_datasource SET mode='DEDICATED',status='PENDING',"
                    + "jdbc_url_enc=COALESCE(?,jdbc_url_enc),"
                    + "jdbc_username_enc=COALESCE(?,jdbc_username_enc),"
                    + "jdbc_password_enc=COALESCE(?,jdbc_password_enc),"
                    + "key_id=?,pool_max_size=?,last_error=NULL,updated_at=? WHERE tenant_id=?",
                    urlEnc, userEnc, passEnc, cipher.keyId(), req.getPoolMaxSize(), ts(), tenantId);
        } else {
            jdbc.update("INSERT INTO tenant_datasource (tenant_id,mode,status,"
                    + "jdbc_url_enc,jdbc_username_enc,jdbc_password_enc,key_id,pool_max_size,created_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?)",
                    tenantId, "DEDICATED", "PENDING", urlEnc, userEnc, passEnc,
                    cipher.keyId(), req.getPoolMaxSize(), ts());
        }
        catalog.invalidate(tenantId);
        return get(tenantId);
    }

    /**
     * Open a throwaway connection to validate config.
     *
     * <p>If {@code probe} is supplied with a JDBC URL it dials those creds
     * directly (lets the operator test before saving). Otherwise the saved
     * row's encrypted creds are decrypted in memory and dialled — supplied
     * password takes precedence over the stored one, supplied username
     * defaults to the stored value if blank, so the operator can also test a
     * password without persisting it.
     *
     * <p>Never echoes credentials in any return value or log.
     */
    public boolean testConnection(String tenantId, TenantDatasourceRequest probe) {
        if (probe != null && StringUtils.hasText(probe.getJdbcUrl())) {
            return dial(probe.getJdbcUrl(), probe.getUsername(), probe.getPassword());
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT jdbc_url_enc,jdbc_username_enc,jdbc_password_enc "
                + "FROM tenant_datasource WHERE tenant_id=?", tenantId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No datasource configured for tenant " + tenantId
                    + " — fill in the JDBC URL/username/password and test, or save first.");
        }
        Map<String, Object> r = rows.get(0);
        String storedUrl  = cipher.decrypt((String) r.get("jdbc_url_enc"));
        String storedUser = cipher.decrypt((String) r.get("jdbc_username_enc"));
        String storedPass = cipher.decrypt((String) r.get("jdbc_password_enc"));
        String useUser = (probe != null && StringUtils.hasText(probe.getUsername())) ? probe.getUsername() : storedUser;
        String usePass = (probe != null && StringUtils.hasText(probe.getPassword())) ? probe.getPassword() : storedPass;
        return dial(storedUrl, useUser, usePass);
    }

    /** Open one short-lived JDBC connection and report whether it is valid. */
    private boolean dial(String url, String user, String pass) {
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            return c.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private static Timestamp ts() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
