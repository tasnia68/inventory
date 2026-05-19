package com.inventory.system.config.tenant.routing;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Production {@link DedicatedFlywayMigrator}: runs the same
 * {@code classpath:db/migration} scripts against the dedicated tenant DB with
 * the same settings as the shared DB ({@code baseline-on-migrate=true}).
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class FlywayDedicatedMigrator implements DedicatedFlywayMigrator {

    @Override
    public String migrateToBaseline(String jdbcUrl, String username, String password) {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        MigrationInfo current = flyway.info().current();
        if (current == null || current.getVersion() == null) {
            throw new IllegalStateException("Flyway reported no applied version after migrate");
        }
        return current.getVersion().getVersion();
    }
}
