package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantDataSourceRegistryTest {

    private TenantRoutingProperties props(int cacheMax) {
        TenantRoutingProperties p = new TenantRoutingProperties();
        p.setPoolCacheMaxSize(cacheMax);
        p.setPoolMaxSize(2);
        p.setPoolMinIdle(0);
        p.setConnectionTimeoutMs(1500);
        p.setValidationTimeoutMs(300);
        p.setIdleTimeoutMs(10_000);
        return p;
    }

    private HikariDataSource h2(String name) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    private ResolvedRouting dedicated(String key, String h2name) {
        return ResolvedRouting.dedicated(key,
                "jdbc:h2:mem:" + h2name + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa", "", "h2:" + h2name);
    }

    @Test
    void sharedDataSourceIsReturnedAsIs() {
        HikariDataSource shared = h2("shared_a");
        TenantDataSourceRegistry reg = new TenantDataSourceRegistry(shared, props(10));
        assertThat(reg.sharedDataSource()).isSameAs(shared);
        shared.close();
    }

    @Test
    void unreachableDedicatedFailsClosed() {
        TenantDataSourceRegistry reg = new TenantDataSourceRegistry(h2("shared_b"), props(10));
        ResolvedRouting bad = ResolvedRouting.dedicated("acme",
                "jdbc:postgresql://127.0.0.1:1/nope", "u", "p", "127.0.0.1:1");
        assertThatThrownBy(() -> reg.dedicatedDataSource(bad))
                .isInstanceOf(TenantDatasourceUnavailableException.class);
    }

    @Test
    void dedicatedPoolIsCreatedReusedAndUsable() throws Exception {
        TenantDataSourceRegistry reg = new TenantDataSourceRegistry(h2("shared_c"), props(10));
        DataSource a1 = reg.dedicatedDataSource(dedicated("t1", "ded_1"));
        DataSource a2 = reg.dedicatedDataSource(dedicated("t1", "ded_1"));
        assertThat(a1).isSameAs(a2);
        try (Connection c = a1.getConnection()) {
            assertThat(c.isValid(1)).isTrue();
        }
        reg.closeAll();
    }

    @Test
    void lruEvictionClosesColdIdlePool() {
        TenantDataSourceRegistry reg = new TenantDataSourceRegistry(h2("shared_d"), props(1));
        HikariDataSource first =
                (HikariDataSource) reg.dedicatedDataSource(dedicated("t1", "lru_1"));
        // exceeding cap of 1 evicts the eldest (t1), which has no active leases
        reg.dedicatedDataSource(dedicated("t2", "lru_2"));
        assertThat(first.isClosed()).isTrue();
        reg.closeAll();
    }

    @Test
    void explicitEvictClosesPool() {
        TenantDataSourceRegistry reg = new TenantDataSourceRegistry(h2("shared_e"), props(10));
        HikariDataSource ds =
                (HikariDataSource) reg.dedicatedDataSource(dedicated("t9", "ev_9"));
        reg.evict("t9");
        assertThat(ds.isClosed()).isTrue();
        reg.closeAll();
    }
}
