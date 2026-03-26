package com.inventory.system.config;

import com.inventory.system.config.tenant.TenantContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfiguration {

    @Component("auditorProvider")
    public static class AuditorProvider implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String tenantId = TenantContext.getTenantId();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken
                    || !StringUtils.hasText(authentication.getName())) {
                return Optional.of(qualifyAuditor(tenantId, "system"));
            }

            return Optional.of(qualifyAuditor(tenantId, authentication.getName()));
        }

        private String qualifyAuditor(String tenantId, String principal) {
            if (!StringUtils.hasText(tenantId)) {
                return principal;
            }
            return tenantId + ":" + principal;
        }
    }
}
