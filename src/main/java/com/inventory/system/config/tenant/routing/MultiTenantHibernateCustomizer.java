package com.inventory.system.config.tenant.routing;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Phase 2 wiring: enables Hibernate {@code DATABASE} multi-tenancy by handing
 * Hibernate our {@link MultiTenantConnectionProviderImpl} and
 * {@link CurrentTenantResolver}.
 *
 * <p>This deliberately uses a {@link HibernatePropertiesCustomizer} rather than
 * a hand-rolled {@code EntityManagerFactory}: Spring Boot still builds the
 * persistence unit with all its existing settings (dialect, ddl-auto=validate,
 * naming strategy, JPA auditing, OSIV, Flyway ordering) — we only add the two
 * multi-tenancy properties. In Hibernate 6 supplying a
 * {@code MultiTenantConnectionProvider} is what activates DATABASE
 * multi-tenancy (the old {@code hibernate.multiTenancy} strategy enum was
 * removed in 6.0).
 *
 * <p>Strictly gated by {@code app.tenant.routing.enabled}. While the flag is
 * off (Phases 0/1 and the production default) this bean does not exist, so the
 * EMF is built exactly as before — that is what makes Phase 2 a behavioural
 * no-op until explicitly enabled. When on, every tenant still resolves to the
 * shared pool until a {@code tenant_datasource} row marks one DEDICATED.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class MultiTenantHibernateCustomizer {

    @Bean
    public HibernatePropertiesCustomizer multiTenantHibernatePropertiesCustomizer(
            MultiTenantConnectionProviderImpl connectionProvider,
            CurrentTenantResolver tenantResolver) {
        return (Map<String, Object> props) -> {
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}
