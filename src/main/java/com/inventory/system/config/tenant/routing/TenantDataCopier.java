package com.inventory.system.config.tenant.routing;

import java.util.Map;

/**
 * Copies a single tenant's rows from the shared database into its (already
 * schema-migrated) dedicated database. Extracted as a seam so the cutover
 * orchestration is unit-testable without two real Postgres instances; the
 * production implementation is a JDBC table-by-table copy.
 */
public interface TenantDataCopier {

    /**
     * @return per-table copy report: table name → {@link TableCount} of the
     *         shared-side source count and the dedicated-side written count.
     *         The cutover refuses to flip unless every pair is equal.
     */
    Map<String, TableCount> copyTenant(String tenantId, String dedicatedUrl,
                                       String username, String password);

    record TableCount(long source, long copied) {
        boolean matches() {
            return source == copied;
        }
    }
}
