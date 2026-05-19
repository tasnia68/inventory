package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantCatalogServiceTest {

    private HikariDataSource ds;
    private JdbcTemplate jdbc;
    private CredentialCipher cipher;
    private TenantRoutingProperties props;
    private TenantCatalogService service;

    @BeforeEach
    void setUp() {
        ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:catalog_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUsername("sa");
        ds.setPassword("");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
            CREATE TABLE tenant_datasource (
              tenant_id VARCHAR(255) PRIMARY KEY,
              mode VARCHAR(16) NOT NULL,
              status VARCHAR(16) NOT NULL,
              jdbc_url_enc TEXT, jdbc_username_enc TEXT, jdbc_password_enc TEXT,
              key_id VARCHAR(64), pool_max_size INT, pool_min_idle INT,
              idle_timeout_ms BIGINT, flyway_version VARCHAR(32),
              provisioned_at TIMESTAMP, last_migrated_at TIMESTAMP,
              last_routed_at TIMESTAMP, last_error TEXT,
              created_at TIMESTAMP NOT NULL, created_by VARCHAR(255),
              updated_at TIMESTAMP, updated_by VARCHAR(255))
            """);

        props = new TenantRoutingProperties();
        props.setKek("unit-test-kek");
        cipher = new CredentialCipher(props);
        service = new TenantCatalogService(ds, cipher, props);
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    private void insert(String tenant, String mode, String status,
                        String urlEnc, String userEnc, String passEnc) {
        jdbc.update("INSERT INTO tenant_datasource "
                + "(tenant_id,mode,status,jdbc_url_enc,jdbc_username_enc,jdbc_password_enc,created_at) "
                + "VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                tenant, mode, status, urlEnc, userEnc, passEnc);
    }

    @Test
    void absentRowResolvesToShared() {
        ResolvedRouting r = service.resolve("acme");
        assertThat(r.shared()).isTrue();
        assertThat(r.routingKey()).isEqualTo(props.getSharedIdentifier());
    }

    @Test
    void nullOrSentinelResolvesToSharedWithoutDbHit() {
        assertThat(service.resolve(null).shared()).isTrue();
        assertThat(service.resolve(props.getSharedIdentifier()).shared()).isTrue();
    }

    @Test
    void sharedActiveRowResolvesToShared() {
        insert("acme", "SHARED", "ACTIVE", null, null, null);
        assertThat(service.resolve("acme").shared()).isTrue();
    }

    @Test
    void dedicatedActiveResolvesWithDecryptedCreds() {
        insert("acme", "DEDICATED", "ACTIVE",
                cipher.encrypt("jdbc:postgresql://acme-db:5432/acme"),
                cipher.encrypt("acme_user"),
                cipher.encrypt("s3cr3t"));

        ResolvedRouting r = service.resolve("acme");
        assertThat(r.shared()).isFalse();
        assertThat(r.routingKey()).isEqualTo("acme");
        assertThat(r.jdbcUrl()).isEqualTo("jdbc:postgresql://acme-db:5432/acme");
        assertThat(r.username()).isEqualTo("acme_user");
        assertThat(r.password()).isEqualTo("s3cr3t");
        assertThat(r.host()).isEqualTo("acme-db:5432");
    }

    @Test
    void nonActiveStatusFailsClosed() {
        for (String status : new String[]{"PENDING", "MIGRATING", "DISABLED"}) {
            insert("t-" + status, "DEDICATED", status, null, null, null);
            assertThatThrownBy(() -> service.resolve("t-" + status))
                    .isInstanceOf(TenantDatasourceUnavailableException.class)
                    .hasMessageContaining(status);
        }
    }

    @Test
    void dedicatedWithMissingCredsFailsClosed() {
        insert("acme", "DEDICATED", "ACTIVE", null, null, null);
        assertThatThrownBy(() -> service.resolve("acme"))
                .isInstanceOf(TenantDatasourceUnavailableException.class);
    }

    @Test
    void cachedThenInvalidatedRereads() {
        insert("acme", "SHARED", "ACTIVE", null, null, null);
        assertThat(service.resolve("acme").shared()).isTrue();      // cached
        jdbc.update("UPDATE tenant_datasource SET status='DISABLED' WHERE tenant_id='acme'");
        assertThat(service.resolve("acme").shared()).isTrue();      // still cached
        service.invalidate("acme");
        assertThatThrownBy(() -> service.resolve("acme"))           // re-read: now fails closed
                .isInstanceOf(TenantDatasourceUnavailableException.class);
    }
}
