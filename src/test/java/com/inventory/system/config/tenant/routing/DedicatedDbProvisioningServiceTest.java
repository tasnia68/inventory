package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DedicatedDbProvisioningServiceTest {

    /** Records what it was asked to migrate; never touches a real DB. */
    static class RecordingMigrator implements DedicatedFlywayMigrator {
        String url, user, pass;
        boolean fail;
        @Override public String migrateToBaseline(String jdbcUrl, String username, String password) {
            this.url = jdbcUrl; this.user = username; this.pass = password;
            if (fail) throw new IllegalStateException("boom");
            return "59";
        }
    }

    private HikariDataSource shared;
    private JdbcTemplate jdbc;
    private CredentialCipher cipher;
    private RecordingMigrator migrator;
    private DedicatedDbProvisioningService svc;
    private String sharedUrl;

    @BeforeEach
    void setUp() {
        sharedUrl = "jdbc:h2:mem:prov_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        shared = new HikariDataSource();
        shared.setJdbcUrl(sharedUrl);
        shared.setUsername("sa");
        shared.setPassword("");
        jdbc = new JdbcTemplate(shared);
        jdbc.execute("""
            CREATE TABLE tenant_datasource (
              tenant_id VARCHAR(255) PRIMARY KEY, mode VARCHAR(16) NOT NULL,
              status VARCHAR(16) NOT NULL, jdbc_url_enc TEXT,
              jdbc_username_enc TEXT, jdbc_password_enc TEXT, key_id VARCHAR(64),
              pool_max_size INT, pool_min_idle INT, idle_timeout_ms BIGINT,
              flyway_version VARCHAR(32), provisioned_at TIMESTAMP,
              last_migrated_at TIMESTAMP, last_routed_at TIMESTAMP,
              last_error TEXT, created_at TIMESTAMP NOT NULL,
              created_by VARCHAR(255), updated_at TIMESTAMP, updated_by VARCHAR(255))
            """);
        TenantRoutingProperties props = new TenantRoutingProperties();
        props.setKek("prov-kek");
        cipher = new CredentialCipher(props);
        migrator = new RecordingMigrator();
        TenantCatalogService catalog = new TenantCatalogService(shared, cipher, props);
        svc = new DedicatedDbProvisioningService(shared, cipher, migrator, catalog);
    }

    @AfterEach
    void tearDown() {
        shared.close();
    }

    private void insert(String tenant, String mode, String status, String url) {
        jdbc.update("INSERT INTO tenant_datasource "
                + "(tenant_id,mode,status,jdbc_url_enc,jdbc_username_enc,jdbc_password_enc,created_at) "
                + "VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                tenant, mode, status,
                url == null ? null : cipher.encrypt(url),
                cipher.encrypt("u"), cipher.encrypt("p"));
    }

    @Test
    void missingRowIsRejected() {
        assertThatThrownBy(() -> svc.migrateTenant("ghost"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant_datasource row");
    }

    @Test
    void sharedModeIsRejected() {
        insert("acme", "SHARED", "ACTIVE", null);
        assertThatThrownBy(() -> svc.migrateTenant("acme"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not DEDICATED");
    }

    @Test
    void targetUrlEqualToSharedUrlIsRejected_L10() {
        insert("acme", "DEDICATED", "PENDING", sharedUrl);
        assertThatThrownBy(() -> svc.migrateTenant("acme"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shared database URL");
        assertThat(migrator.url).as("migrator must never be called").isNull();
    }

    @Test
    void happyPathMigratesAndMarksActive() {
        String dedicatedUrl = "jdbc:postgresql://acme-db:5432/acme";
        insert("acme", "DEDICATED", "PENDING", dedicatedUrl);

        String version = svc.migrateTenant("acme");

        assertThat(version).isEqualTo("59");
        assertThat(migrator.url).isEqualTo(dedicatedUrl); // routed to dedicated, not shared
        String status = jdbc.queryForObject(
                "SELECT status FROM tenant_datasource WHERE tenant_id='acme'", String.class);
        String fv = jdbc.queryForObject(
                "SELECT flyway_version FROM tenant_datasource WHERE tenant_id='acme'", String.class);
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(fv).isEqualTo("59");
    }

    @Test
    void failedMigrationLeavesTenantNonActiveWithError() {
        insert("acme", "DEDICATED", "PENDING", "jdbc:postgresql://acme-db:5432/acme");
        migrator.fail = true;

        assertThatThrownBy(() -> svc.migrateTenant("acme"))
                .isInstanceOf(IllegalStateException.class);

        String status = jdbc.queryForObject(
                "SELECT status FROM tenant_datasource WHERE tenant_id='acme'", String.class);
        String err = jdbc.queryForObject(
                "SELECT last_error FROM tenant_datasource WHERE tenant_id='acme'", String.class);
        assertThat(status).isEqualTo("PENDING");          // fail-safe: not ACTIVE
        assertThat(err).contains("boom");
    }
}
