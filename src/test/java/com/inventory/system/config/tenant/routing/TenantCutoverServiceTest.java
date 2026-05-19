package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantCutoverServiceTest {

    static class StubMigrator implements DedicatedFlywayMigrator {
        @Override public String migrateToBaseline(String u, String n, String p) { return "59"; }
    }
    static class StubCopier implements TenantDataCopier {
        Map<String, TableCount> report = new LinkedHashMap<>();
        boolean called;
        @Override public Map<String, TableCount> copyTenant(String t, String u, String n, String p) {
            called = true;
            return report;
        }
    }

    private HikariDataSource shared;
    private JdbcTemplate jdbc;
    private CredentialCipher cipher;
    private StubCopier copier;
    private TenantCutoverService svc;

    @BeforeEach
    void setUp() {
        shared = new HikariDataSource();
        shared.setJdbcUrl("jdbc:h2:mem:cut_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
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
        props.setKek("cutover-kek");
        cipher = new CredentialCipher(props);
        copier = new StubCopier();
        TenantCatalogService catalog = new TenantCatalogService(shared, cipher, props);
        TenantDataSourceRegistry registry = new TenantDataSourceRegistry(shared, props);
        svc = new TenantCutoverService(shared, cipher, new StubMigrator(),
                copier, catalog, registry);
    }

    @AfterEach
    void tearDown() {
        shared.close();
    }

    private void insertDedicatedPending() {
        jdbc.update("INSERT INTO tenant_datasource "
                + "(tenant_id,mode,status,jdbc_url_enc,jdbc_username_enc,jdbc_password_enc,created_at) "
                + "VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                "acme", "DEDICATED", "PENDING",
                cipher.encrypt("jdbc:postgresql://acme-db:5432/acme"),
                cipher.encrypt("u"), cipher.encrypt("p"));
    }

    private String status() {
        return jdbc.queryForObject(
                "SELECT status FROM tenant_datasource WHERE tenant_id='acme'", String.class);
    }

    @Test
    void allCountsMatch_flipsToActive() {
        insertDedicatedPending();
        copier.report.put("sales_orders", new TenantDataCopier.TableCount(10, 10));
        copier.report.put("products", new TenantDataCopier.TableCount(5, 5));

        TenantCutoverService.CutoverResult res = svc.cutover("acme");

        assertThat(res.schemaVersion()).isEqualTo("59");
        assertThat(status()).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT flyway_version FROM tenant_datasource WHERE tenant_id='acme'", String.class))
                .isEqualTo("59");
    }

    @Test
    void countMismatch_doesNotFlip_andRollsBack() {
        insertDedicatedPending();
        copier.report.put("sales_orders", new TenantDataCopier.TableCount(10, 9)); // short copy

        assertThatThrownBy(() -> svc.cutover("acme"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verification failed");

        assertThat(status()).isEqualTo("PENDING");          // NOT ACTIVE — fails closed
        assertThat(jdbc.queryForObject(
                "SELECT last_error FROM tenant_datasource WHERE tenant_id='acme'", String.class))
                .contains("sales_orders");
        assertThat(jdbc.queryForObject(
                "SELECT flyway_version FROM tenant_datasource WHERE tenant_id='acme'", String.class))
                .isNull();                                   // never flipped
    }

    @Test
    void sharedModeRejected() {
        jdbc.update("INSERT INTO tenant_datasource (tenant_id,mode,status,created_at) "
                + "VALUES ('acme','SHARED','ACTIVE',CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> svc.cutover("acme"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEDICATED");
        assertThat(copier.called).isFalse();
    }
}
