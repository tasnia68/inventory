package com.inventory.system.config.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ensures the Hibernate {@code tenantFilter} is active on the <em>current</em>
 * {@link Session} at the start of every {@code @Transactional} method.
 *
 * <p>The servlet-level {@link TenantContextFilter} sets the filter on the
 * session available at filter time, but Spring's OSIV interceptor may supply a
 * <em>different</em> session to the service layer. This aspect closes that gap
 * by re-enabling the filter on whatever session Hibernate is actually using
 * when the transactional proxy is entered.</p>
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)")
    public void enableTenantFilter(JoinPoint joinPoint) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (Exception ignored) {
            // If we cannot unwrap a session (e.g. no active transaction yet),
            // the servlet filter's enablement is still the fallback.
        }
    }
}
