package com.inventory.system.common.entity;

/**
 * Routing mode for a tenant. The absence of a {@code tenant_datasource} row is
 * treated as {@link #SHARED}; only explicitly-configured tenants are DEDICATED.
 */
public enum TenantDatasourceMode {
    /** Tenant lives in the shared schema (default), isolated by the tenant_id @Filter. */
    SHARED,
    /** Tenant has its own database (own Postgres instance). */
    DEDICATED
}
