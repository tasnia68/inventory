package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 3 mandatory fail-closed test: a DEDICATED tenant whose DB is
 * unreachable must cause the connection provider to throw — recording a
 * DENIED audit — and must NEVER return the shared connection as a fallback.
 * The shared path must keep working for everyone else.
 */
class MultiTenantConnectionProviderFailClosedTest {

    private HikariDataSource shared;
    private JdbcTemplate jdbc;
    private MultiTenantConnectionProviderImpl provider;

    @BeforeEach
    void setUp() {
        shared = new HikariDataSource();
        shared.setJdbcUrl("jdbc:h2:mem:fc_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
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
        jdbc.execute("""
            CREATE TABLE tenant_routing_audit (
              id UUID PRIMARY KEY, tenant_id VARCHAR(255) NOT NULL,
              decision VARCHAR(16) NOT NULL, reason TEXT, jdbc_url_host VARCHAR(255),
              principal VARCHAR(255), request_path VARCHAR(512),
              created_at TIMESTAMP NOT NULL)
            """);

        TenantRoutingProperties props = new TenantRoutingProperties();
        props.setKek("fail-closed-kek");
        props.setConnectionTimeoutMs(1500);
        props.setValidationTimeoutMs(300);
        CredentialCipher cipher = new CredentialCipher(props);

        // DEDICATED + ACTIVE, but the URL points nowhere.
        jdbc.update("INSERT INTO tenant_datasource "
                + "(tenant_id,mode,status,jdbc_url_enc,jdbc_username_enc,jdbc_password_enc,created_at) "
                + "VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                "acme", "DEDICATED", "ACTIVE",
                cipher.encrypt("jdbc:postgresql://127.0.0.1:1/nope"),
                cipher.encrypt("u"), cipher.encrypt("p"));

        TenantCatalogService catalog = new TenantCatalogService(shared, cipher, props);
        TenantDataSourceRegistry registry = new TenantDataSourceRegistry(shared, props);
        RoutingAuditService audit = new RoutingAuditService(shared);
        provider = new MultiTenantConnectionProviderImpl(catalog, registry, audit);
    }

    @AfterEach
    void tearDown() {
        shared.close();
    }

    @Test
    void dedicatedDbDownThrowsAndAuditsDenied_neverFallsBackToShared() {
        long sharedRowsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenant_datasource", Long.class);

        assertThatThrownBy(() -> provider.getConnection("acme"))
                .isInstanceOf(TenantDatasourceUnavailableException.class);

        // a DENIED audit row was written for this tenant
        Long denied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenant_routing_audit WHERE tenant_id='acme' AND decision='DENIED'",
                Long.class);
        assertThat(denied).isEqualTo(1L);

        // shared DB was NOT touched as a fallback (unchanged) and still works
        long sharedRowsAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenant_datasource", Long.class);
        assertThat(sharedRowsAfter).isEqualTo(sharedRowsBefore);
    }

    @Test
    void sharedTenantStillWorksWhileADedicatedTenantIsDown() throws Exception {
        try (Connection c = provider.getConnection("__shared__")) {
            assertThat(c.isValid(1)).isTrue();
        }
        try (Connection c = provider.getAnyConnection()) {
            assertThat(c.isValid(1)).isTrue();
        }
    }
}
