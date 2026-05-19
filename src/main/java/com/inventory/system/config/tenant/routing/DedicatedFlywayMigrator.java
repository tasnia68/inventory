package com.inventory.system.config.tenant.routing;

/**
 * Seam over the Flyway migration of a dedicated tenant database. Extracted so
 * the provisioning orchestration (guards, lifecycle, catalog updates) is
 * unit-testable without a real Postgres / Docker; the production
 * implementation is a thin Flyway call.
 */
public interface DedicatedFlywayMigrator {

    /**
     * Migrate the target database to the application baseline using the same
     * {@code classpath:db/migration} scripts as the shared DB.
     *
     * @return the resulting schema version (e.g. "59")
     */
    String migrateToBaseline(String jdbcUrl, String username, String password);
}
