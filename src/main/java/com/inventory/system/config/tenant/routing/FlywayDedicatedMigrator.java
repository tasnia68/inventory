package com.inventory.system.config.tenant.routing;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Arrays;
import java.util.Properties;

/**
 * Production {@link DedicatedFlywayMigrator}.
 *
 * <p>The application's Flyway migration set was historically a thin layer over
 * Hibernate's auto-generated schema: tables like {@code sales_orders} are
 * referenced by later migrations (e.g. V10) but never created by an earlier
 * migration script — they came from a long-ago {@code ddl-auto=update} pass on
 * the shared DB. So Flyway alone cannot bootstrap an empty dedicated database.
 *
 * <p>This migrator does what the shared DB had done historically:
 * <ol>
 *   <li>If the target is empty, materialize the schema from the current
 *       {@code @Entity} classes via a short-lived EntityManagerFactory with
 *       {@code hibernate.hbm2ddl.auto=create}. Same code-of-truth as the
 *       running app.</li>
 *   <li>Configure Flyway with {@code baselineOnMigrate=true} and the
 *       {@code baselineVersion} set to the highest available migration on
 *       classpath. Flyway records all existing {@code V*__*.sql} as applied
 *       (no replay) — future deltas (V&lt;next&gt;+) will run normally.</li>
 *   <li>If the target already has tables (re-running on a previously
 *       bootstrapped dedicated DB), skip the Hibernate step and let Flyway do
 *       its normal incremental migration.</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class FlywayDedicatedMigrator implements DedicatedFlywayMigrator {

    private static final Logger log = LoggerFactory.getLogger(FlywayDedicatedMigrator.class);

    /** Package containing every {@code @Entity} class to materialize. */
    private static final String ENTITY_PACKAGE = "com.inventory.system";

    @Override
    public String migrateToBaseline(String jdbcUrl, String username, String password) {
        boolean empty = isSchemaEmpty(jdbcUrl, username, password);
        if (empty) {
            log.info("Dedicated DB is empty — bootstrapping schema from @Entity metadata");
            bootstrapSchemaWithHibernate(jdbcUrl, username, password);
        }

        String latest = highestAvailableMigrationVersion(jdbcUrl, username, password);

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(latest)
                .load();
        flyway.migrate();

        MigrationInfo current = flyway.info().current();
        String reported = (current == null || current.getVersion() == null) ? latest : current.getVersion().getVersion();
        log.info("Dedicated DB ready at schema version {}", reported);
        return reported;
    }

    /** Empty = no user tables in the public schema (history table also absent). */
    private boolean isSchemaEmpty(String url, String user, String pass) {
        try (Connection c = openConnection(url, user, pass);
             var ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'public'");
             var rs = ps.executeQuery()) {
            return rs.next() && rs.getLong(1) == 0;
        } catch (Exception e) {
            // Be conservative: if we can't tell, treat as non-empty and let
            // Flyway error out cleanly instead of corrupting an existing DB.
            log.warn("Could not inspect dedicated DB tables, assuming non-empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build a short-lived EMF pointing at the dedicated DB with
     * {@code hbm2ddl.auto=create}, which forces Hibernate to materialize every
     * mapped entity's table on EMF startup. Then close it cleanly.
     */
    private void bootstrapSchemaWithHibernate(String url, String user, String pass) {
        DataSource ds = newDataSource(url, user, pass);

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan(ENTITY_PACKAGE);
        emf.setPersistenceUnitName("dedicated-bootstrap");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpa = new Properties();
        jpa.put("hibernate.hbm2ddl.auto", "create");
        jpa.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        // Match Spring Boot's defaults so column/table/FK names are IDENTICAL
        // to the shared DB (camelCase fields -> snake_case columns). Without
        // this the bootstrap creates e.g. `firstname` instead of `first_name`
        // and the data copy collides with non-existent columns.
        jpa.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        jpa.put("hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        // Multi-tenancy is NOT enabled here on purpose: we are creating the
        // raw physical schema for this one DB, not driving a tenanted session.
        emf.setJpaProperties(jpa);

        emf.afterPropertiesSet();   // triggers schema generation
        try {
            // No queries — opening + closing is enough; create runs on init.
        } finally {
            emf.destroy();
        }
        log.info("Dedicated DB schema materialized from @Entity classes ({})", ENTITY_PACKAGE);
    }

    private String highestAvailableMigrationVersion(String url, String user, String pass) {
        // Flyway is the source of truth for which V*__*.sql files exist on classpath.
        Flyway scan = Flyway.configure()
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .load();
        MigrationInfo[] all = scan.info().all();
        return Arrays.stream(all)
                .map(MigrationInfo::getVersion)
                .filter(v -> v != null)
                .map(Object::toString)
                .max(this::compareVersions)
                .orElseThrow(() -> new IllegalStateException(
                        "No migrations found on classpath:db/migration — cannot baseline dedicated DB"));
    }

    /** Compare semantic-ish migration versions ("59" > "9" etc.) numerically. */
    private int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            long va = i < pa.length ? Long.parseLong(pa[i].replaceAll("\\D", "")) : 0;
            long vb = i < pb.length ? Long.parseLong(pb[i].replaceAll("\\D", "")) : 0;
            if (va != vb) return Long.compare(va, vb);
        }
        return 0;
    }

    private DataSource newDataSource(String url, String user, String pass) {
        try {
            Class<?> driverClass = Class.forName("org.postgresql.Driver");
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            return new SimpleDriverDataSource(driver, url, user, pass);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load Postgres JDBC driver for dedicated bootstrap", e);
        }
    }

    private Connection openConnection(String url, String user, String pass) throws Exception {
        return newDataSource(url, user, pass).getConnection();
    }
}
