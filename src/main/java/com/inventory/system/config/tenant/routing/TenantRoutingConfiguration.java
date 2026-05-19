package com.inventory.system.config.tenant.routing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the hybrid multi-tenant routing layer.
 *
 * <p>Phase 0/1: this only registers {@link TenantRoutingProperties} and lets
 * the catalog/cipher beans be component-scanned. Nothing is attached to
 * Hibernate yet, so application behaviour is unchanged. The Hibernate
 * {@code DATABASE} multitenancy wiring is added in Phase 2 behind
 * {@code app.tenant.routing.enabled}.
 */
@Configuration
@EnableConfigurationProperties(TenantRoutingProperties.class)
public class TenantRoutingConfiguration {
}
