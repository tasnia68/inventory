package com.inventory.system.config.tenant.routing;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hibernate connection provider for hybrid (Bridge) multi-tenancy.
 *
 * <p>Resolves each tenant via {@link TenantCatalogService}; SHARED tenants use
 * the Spring-managed primary pool, DEDICATED tenants use their own pool from
 * {@link TenantDataSourceRegistry}.
 *
 * <p><strong>Fail-closed:</strong> if a DEDICATED tenant cannot be served the
 * underlying services throw {@link TenantDatasourceUnavailableException}; this
 * provider records a DENIED audit and rethrows. It NEVER returns the shared
 * connection for a DEDICATED tenant.
 *
 * <p>Gated by {@code app.tenant.routing.enabled}. In Phases 0/1 the flag is
 * off so this bean is not created and Hibernate never sees it — behaviour is
 * unchanged. Phase 2 wires it into the EntityManagerFactory.
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider<String> {

    private final TenantCatalogService catalog;
    private final TenantDataSourceRegistry registry;
    private final RoutingAuditService audit;

    public MultiTenantConnectionProviderImpl(TenantCatalogService catalog,
                                             TenantDataSourceRegistry registry,
                                             RoutingAuditService audit) {
        this.catalog = catalog;
        this.registry = registry;
        this.audit = audit;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return registry.sharedDataSource().getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        ResolvedRouting routing;
        try {
            routing = catalog.resolve(tenantIdentifier);
        } catch (TenantDatasourceUnavailableException e) {
            // Fail-closed: record and rethrow; never substitute the shared DB.
            audit.record(tenantIdentifier, RoutingDecision.DENIED, e.getMessage(),
                    null, null, null);
            throw e;
        }

        if (routing.shared()) {
            // The default, unchanged path — not audited per-connection to avoid
            // an audit row on every query.
            return registry.sharedDataSource().getConnection();
        }

        DataSource ds;
        try {
            ds = registry.dedicatedDataSource(routing);
        } catch (TenantDatasourceUnavailableException e) {
            audit.record(tenantIdentifier, RoutingDecision.DENIED, e.getMessage(),
                    routing.host(), null, null);
            throw e;
        }
        audit.record(tenantIdentifier, RoutingDecision.DEDICATED, "routed",
                routing.host(), null, null);
        return ds.getConnection();
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
