package com.inventory.system.config.tenant.routing;

import com.inventory.system.config.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentTenantResolverTest {

    private final TenantRoutingProperties props = new TenantRoutingProperties();
    private final CurrentTenantResolver resolver = new CurrentTenantResolver(props);

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantWhenContextSet() {
        TenantContext.setTenantId("acme");
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("acme");
    }

    @Test
    void fallsBackToSharedSentinelWhenNoContext() {
        TenantContext.clear();
        assertThat(resolver.resolveCurrentTenantIdentifier())
                .isEqualTo(props.getSharedIdentifier());
    }

    @Test
    void validatesExistingSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
    }
}
