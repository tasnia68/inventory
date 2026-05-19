package com.inventory.system.config.tenant.routing;

import com.inventory.system.config.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Supplies Hibernate's current tenant identifier from {@link TenantContext}
 * (the same server-validated value used by the discriminator filter). Never
 * returns null: absent context resolves to the shared sentinel, so a request
 * without a tenant can only ever touch the shared pool (and is still filtered
 * by the always-on {@code tenantFilter}).
 *
 * <p>Gated by {@code app.tenant.routing.enabled} — inert until Phase 2.
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

    private final TenantRoutingProperties props;

    public CurrentTenantResolver(TenantRoutingProperties props) {
        this.props = props;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getCurrentTenantId();
        return StringUtils.hasText(tenantId) ? tenantId : props.getSharedIdentifier();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
