package com.inventory.system.config.tenant;

import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class TenantContextFilter implements Filter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final EntityManager entityManager;

    public TenantContextFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String tenantId = req.getHeader(TENANT_HEADER);

        if (!StringUtils.hasText(tenantId)) {
            // Strict Mode: Require Tenant ID
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-ID header");
            return;
        }

        TenantContext.setTenantId(tenantId);
        try {
            // Enable the Hibernate Filter for this request/session
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (Exception e) {
            // If we cannot enable the filter, we must block the request to prevent data leakage
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to initialize tenant context");
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
