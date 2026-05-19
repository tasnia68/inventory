package com.inventory.system.config.tenant.routing;

import com.inventory.system.payload.TenantDatasourceRequest;
import com.inventory.system.payload.TenantDatasourceResponse;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantDatasourceAdminServiceTest {

    private HikariDataSource shared;
    private JdbcTemplate jdbc;
    private TenantDatasourceAdminService svc;

    @BeforeEach
    void setUp() {
        shared = new HikariDataSource();
        shared.setJdbcUrl("jdbc:h2:mem:adm_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
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
        props.setKek("admin-kek");
        CredentialCipher cipher = new CredentialCipher(props);
        TenantCatalogService catalog = new TenantCatalogService(shared, cipher, props);
        svc = new TenantDatasourceAdminService(shared, cipher, catalog);
    }

    @AfterEach
    void tearDown() {
        shared.close();
    }

    @Test
    void absentTenantReadsAsImplicitShared() {
        TenantDatasourceResponse r = svc.get("ghost");
        assertThat(r.getMode()).isEqualTo("SHARED");
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
        assertThat(r.isCredentialsConfigured()).isFalse();
    }

    @Test
    void upsertDedicatedStoresEncryptedAndNeverReturnsCreds() throws Exception {
        TenantDatasourceRequest req = new TenantDatasourceRequest();
        req.setMode("DEDICATED");
        req.setJdbcUrl("jdbc:postgresql://secret-host:5432/acme");
        req.setUsername("acme_user");
        req.setPassword("sup3r-s3cret");

        TenantDatasourceResponse r = svc.upsert("acme", req);

        // Response carries no credentials, only host + flags
        assertThat(r.getMode()).isEqualTo("DEDICATED");
        assertThat(r.getStatus()).isEqualTo("PENDING");
        assertThat(r.getHost()).isEqualTo("secret-host:5432");
        assertThat(r.isCredentialsConfigured()).isTrue();
        for (Field f : TenantDatasourceResponse.class.getDeclaredFields()) {
            f.setAccessible(true);
            Object v = f.get(r);
            if (v instanceof String s) {
                assertThat(s).doesNotContain("sup3r-s3cret");
                assertThat(s).doesNotContain("acme_user");
            }
        }
        // Stored ciphertext is not the plaintext
        String urlEnc = jdbc.queryForObject(
                "SELECT jdbc_url_enc FROM tenant_datasource WHERE tenant_id='acme'", String.class);
        assertThat(urlEnc).doesNotContain("secret-host");
    }

    @Test
    void revertToSharedClearsCredentials() {
        TenantDatasourceRequest ded = new TenantDatasourceRequest();
        ded.setMode("DEDICATED");
        ded.setJdbcUrl("jdbc:postgresql://h/db");
        ded.setUsername("u");
        ded.setPassword("p");
        svc.upsert("acme", ded);

        TenantDatasourceRequest shared = new TenantDatasourceRequest();
        shared.setMode("SHARED");
        TenantDatasourceResponse r = svc.upsert("acme", shared);

        assertThat(r.getMode()).isEqualTo("SHARED");
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
        assertThat(r.isCredentialsConfigured()).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT jdbc_url_enc FROM tenant_datasource WHERE tenant_id='acme'", String.class))
                .isNull();
    }
}
