package com.inventory.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfiguration {

    @Component("auditorProvider")
    public static class AuditorProvider implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: Retrieve the current user from Spring Security context
            // For now, returning a placeholder
            return Optional.of("system");
        }
    }
}
