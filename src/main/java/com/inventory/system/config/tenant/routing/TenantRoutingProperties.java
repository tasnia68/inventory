package com.inventory.system.config.tenant.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for hybrid (Bridge) multi-tenant datasource routing.
 *
 * <p>{@code enabled} is the master feature flag. While {@code false} (the
 * default), none of the routing infrastructure is attached to Hibernate and
 * the application behaves exactly as before. The flag is only honoured from
 * Phase 2 onward.
 */
@ConfigurationProperties(prefix = "app.tenant.routing")
public class TenantRoutingProperties {

    /** Master switch. Default OFF — Phases 0/1 build infra without activating it. */
    private boolean enabled = false;

    /** Hibernate tenant identifier used for every SHARED tenant. */
    private String sharedIdentifier = "__shared__";

    /** Key-encryption key for {@link CredentialCipher}; injected from the environment. */
    private String kek = "";

    /** Logical id of the active KEK, recorded on each encrypted row for rotation. */
    private String keyId = "env:v1";

    /** Default per-dedicated-tenant Hikari pool sizing (overridable per row). */
    private int poolMaxSize = 5;
    private int poolMinIdle = 0;
    private long idleTimeoutMs = 60_000L;
    private long connectionTimeoutMs = 8_000L;
    private long validationTimeoutMs = 4_000L;

    /** Bounded LRU cache of dedicated pools (per app node). */
    private int poolCacheMaxSize = 200;

    /** Catalog cache TTL (seconds) before re-reading tenant_datasource. */
    private long catalogCacheTtlSeconds = 30L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getSharedIdentifier() { return sharedIdentifier; }
    public void setSharedIdentifier(String sharedIdentifier) { this.sharedIdentifier = sharedIdentifier; }

    public String getKek() { return kek; }
    public void setKek(String kek) { this.kek = kek; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public int getPoolMaxSize() { return poolMaxSize; }
    public void setPoolMaxSize(int poolMaxSize) { this.poolMaxSize = poolMaxSize; }

    public int getPoolMinIdle() { return poolMinIdle; }
    public void setPoolMinIdle(int poolMinIdle) { this.poolMinIdle = poolMinIdle; }

    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }

    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public long getValidationTimeoutMs() { return validationTimeoutMs; }
    public void setValidationTimeoutMs(long validationTimeoutMs) { this.validationTimeoutMs = validationTimeoutMs; }

    public int getPoolCacheMaxSize() { return poolCacheMaxSize; }
    public void setPoolCacheMaxSize(int poolCacheMaxSize) { this.poolCacheMaxSize = poolCacheMaxSize; }

    public long getCatalogCacheTtlSeconds() { return catalogCacheTtlSeconds; }
    public void setCatalogCacheTtlSeconds(long catalogCacheTtlSeconds) { this.catalogCacheTtlSeconds = catalogCacheTtlSeconds; }
}
