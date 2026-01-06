package com.inventory.system.config.filter;

import com.inventory.system.config.tenant.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Filter to add Tenant ID to the MDC (Mapped Diagnostic Context) for logging purposes.
 */
public class MdcLoggingFilter implements Filter {

    private static final String TENANT_ID_MDC_KEY = "tenant_id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // Assuming TenantContext has already been populated by TenantContextFilter
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                MDC.put(TENANT_ID_MDC_KEY, tenantId);
            } else {
                 // Try to get from header directly if TenantContext is not yet set or for logging purposes
                 if (request instanceof HttpServletRequest) {
                     String headerTenantId = ((HttpServletRequest) request).getHeader("X-Tenant-ID");
                     if (headerTenantId != null) {
                         MDC.put(TENANT_ID_MDC_KEY, headerTenantId);
                     }
                 }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TENANT_ID_MDC_KEY);
        }
    }
}
