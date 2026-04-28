package com.inventory.system.config.tenant;

import com.inventory.system.security.JwtService;
import com.inventory.system.service.StorefrontDomainService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class TenantContextFilter implements Filter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String STOREFRONT_HOST_OVERRIDE_HEADER = "X-Storefront-Host";

    private final EntityManager entityManager;
    private final StorefrontDomainService storefrontDomainService;
    private final JwtService jwtService;

    public TenantContextFilter(EntityManager entityManager, StorefrontDomainService storefrontDomainService, JwtService jwtService) {
        this.entityManager = entityManager;
        this.storefrontDomainService = storefrontDomainService;
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String requestPath = req.getRequestURI();

        if (requestPath.equals("/error")
                || requestPath.startsWith("/api/v1/auth/")
                || requestPath.equals("/api/v1/tenants/register")
                || requestPath.equals("/api/v1/storefront/domains/caddy/validate")) {
            chain.doFilter(request, response);
            return;
        }

        if (requestPath.startsWith("/api/webhooks/")) {
            // Webhook auth is HMAC-based; JWT bypass is already handled in JwtAuthenticationFilter.
            // Tenant context still needs to be established from the path below.
        }

        boolean isPublicImageRequest = requestPath.startsWith("/api/v1/product-images/") && requestPath.endsWith("/file");

        String tenantId;
        if (requestPath.startsWith("/api/v1/storefront/public/")) {
            tenantId = resolveStorefrontTenant(req);
            if (!StringUtils.hasText(tenantId)) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown storefront domain");
                return;
            }
        } else if (requestPath.startsWith("/api/webhooks/")) {
            tenantId = resolveWebhookTenant(requestPath);
            if (!StringUtils.hasText(tenantId)) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Webhook URL must include tenant segment");
                return;
            }
        } else {
            tenantId = resolveTenantId(req);
            if (!StringUtils.hasText(tenantId) && !isPublicImageRequest) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant context");
                return;
            }
        }

        if (StringUtils.hasText(tenantId)) {
            TenantContext.setTenantId(tenantId);
            try {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            } catch (Exception e) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to initialize tenant context");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveWebhookTenant(String requestPath) {
        // Expected shape: /api/webhooks/{provider}/{tenantId}/{resource}
        String[] parts = requestPath.split("/");
        // parts[0]="" parts[1]="api" parts[2]="webhooks" parts[3]=provider parts[4]=tenantId
        return parts.length > 4 ? parts[4] : null;
    }

    private String resolveStorefrontTenant(HttpServletRequest request) {
        String requestHost = request.getServerName();
        if (storefrontDomainService.isLocalDevelopmentHost(requestHost)) {
            String override = request.getHeader(STOREFRONT_HOST_OVERRIDE_HEADER);
            if (StringUtils.hasText(override)) {
                requestHost = override;
            }
        }
        return storefrontDomainService.resolveTenantIdForHost(requestHost).orElse(null);
    }

    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(tenantId)) {
            return tenantId;
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            try {
                String tokenTenantId = jwtService.extractTenantId(authHeader.substring(7));
                if (StringUtils.hasText(tokenTenantId)) {
                    return tokenTenantId;
                }
            } catch (RuntimeException ignored) {
                // Non-JWT bearer values are handled by downstream logic where applicable.
            }
        }

        tenantId = request.getParameter("tenantId");
        return StringUtils.hasText(tenantId) ? tenantId : null;
    }
}
