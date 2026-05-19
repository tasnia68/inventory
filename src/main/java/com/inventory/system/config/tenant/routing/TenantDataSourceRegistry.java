package com.inventory.system.config.tenant.routing;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded LRU cache of per-tenant {@link HikariDataSource} pools.
 *
 * <p>The shared pool (Spring's primary {@link DataSource}) is returned as-is
 * and is never evicted/closed by this registry (Spring owns its lifecycle).
 * Dedicated pools are created lazily, validated on creation, kept in an
 * access-ordered LRU capped at {@code poolCacheMaxSize}, and the evicted
 * pool is closed only when it has no leased connections (otherwise it is
 * retained until idle). Total connections per node are therefore bounded.
 *
 * <p>Gated by {@code app.tenant.routing.enabled} — inert until Phase 2.
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class TenantDataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceRegistry.class);

    private final DataSource sharedDataSource;
    private final TenantRoutingProperties props;

    /** Access-ordered; eldest is the LRU candidate. Guarded by {@code lock}. */
    private final LinkedHashMap<String, HikariDataSource> pools;
    private final Object lock = new Object();

    public TenantDataSourceRegistry(DataSource sharedDataSource, TenantRoutingProperties props) {
        this.sharedDataSource = sharedDataSource;
        this.props = props;
        this.pools = new LinkedHashMap<>(16, 0.75f, true);
    }

    /** Shared pool — Spring-managed, never wrapped/evicted. */
    public DataSource sharedDataSource() {
        return sharedDataSource;
    }

    /**
     * Returns (creating+validating on first use) the dedicated pool for the
     * given resolved routing. Never returns the shared pool as a fallback.
     */
    public DataSource dedicatedDataSource(ResolvedRouting r) {
        synchronized (lock) {
            HikariDataSource existing = pools.get(r.routingKey());
            if (existing != null && !existing.isClosed()) {
                return existing;
            }
            HikariDataSource created = build(r);
            pools.put(r.routingKey(), created);
            evictIfNeeded();
            return created;
        }
    }

    private HikariDataSource build(ResolvedRouting r) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("tenant-" + r.routingKey());
        cfg.setJdbcUrl(r.jdbcUrl());
        cfg.setUsername(r.username());
        cfg.setPassword(r.password());
        cfg.setMaximumPoolSize(props.getPoolMaxSize());
        cfg.setMinimumIdle(props.getPoolMinIdle());
        cfg.setIdleTimeout(props.getIdleTimeoutMs());
        cfg.setConnectionTimeout(props.getConnectionTimeoutMs());
        cfg.setValidationTimeout(props.getValidationTimeoutMs());
        cfg.setConnectionTestQuery("SELECT 1");
        // Fail fast+closed if the dedicated DB is unreachable. Hikari may throw
        // PoolInitializationException at construction OR the validation may
        // fail — translate both to the fail-closed exception.
        HikariDataSource ds = null;
        try {
            ds = new HikariDataSource(cfg);
            try (Connection c = ds.getConnection()) {
                if (!c.isValid(Math.max(1, (int) (props.getValidationTimeoutMs() / 1000)))) {
                    throw new IllegalStateException("validation query failed");
                }
            }
        } catch (TenantDatasourceUnavailableException e) {
            if (ds != null) ds.close();
            throw e;
        } catch (Exception e) {
            if (ds != null) ds.close();
            throw new TenantDatasourceUnavailableException(
                    "Dedicated DB for tenant " + r.routingKey() + " is unreachable — failing closed", e);
        }
        log.info("Created dedicated datasource pool for tenant {} ({})",
                r.routingKey(), r.host());
        return ds;
    }

    /** Evict LRU entries beyond the cap; close only if no leased connections. */
    private void evictIfNeeded() {
        int cap = props.getPoolCacheMaxSize();
        if (pools.size() <= cap) {
            return;
        }
        List<HikariDataSource> toClose = new ArrayList<>();
        Iterator<Map.Entry<String, HikariDataSource>> it = pools.entrySet().iterator();
        while (pools.size() > cap && it.hasNext()) {
            Map.Entry<String, HikariDataSource> eldest = it.next();
            HikariDataSource ds = eldest.getValue();
            if (ds.getHikariPoolMXBean() != null
                    && ds.getHikariPoolMXBean().getActiveConnections() > 0) {
                continue; // in use — keep it; try a later candidate
            }
            it.remove();
            toClose.add(ds);
        }
        for (HikariDataSource ds : toClose) {
            try {
                ds.close();
                log.info("Evicted+closed idle dedicated pool {}", ds.getPoolName());
            } catch (Exception e) {
                log.warn("Error closing evicted pool {}: {}", ds.getPoolName(), e.getMessage());
            }
        }
    }

    /** Drop+close a tenant's pool (used on credential change / cutover). */
    public void evict(String routingKey) {
        synchronized (lock) {
            HikariDataSource ds = pools.remove(routingKey);
            if (ds != null) {
                try { ds.close(); } catch (Exception ignored) { }
            }
        }
    }

    @PreDestroy
    public void closeAll() {
        synchronized (lock) {
            pools.values().forEach(ds -> {
                try { ds.close(); } catch (Exception ignored) { }
            });
            pools.clear();
        }
    }
}
