package com.inventory.system.common.entity;

/**
 * Lifecycle status of a tenant's datasource configuration. Only
 * {@link #ACTIVE} is routable; every other state fails closed (the request
 * errors — it must never silently fall back to the shared DB).
 */
public enum TenantDatasourceStatus {
    /** Dedicated DB row created but not yet provisioned/migrated. */
    PENDING,
    /** Shared→dedicated cutover in progress; tenant traffic is held. */
    MIGRATING,
    /** Provisioned, schema at baseline, routable. */
    ACTIVE,
    /** Operator-disabled; fail closed. */
    DISABLED
}
