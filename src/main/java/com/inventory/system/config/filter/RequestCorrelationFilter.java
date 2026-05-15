package com.inventory.system.config.filter;

import com.inventory.system.config.tenant.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

public class RequestCorrelationFilter implements Filter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final String REQUEST_ID_MDC_KEY = "request_id";
    private static final String TENANT_ID_MDC_KEY = "tenant_id";
    private static final String USER_MDC_KEY = "user_name";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        String tenantId = TenantContext.getCurrentTenantId();
        if (StringUtils.hasText(tenantId)) {
            MDC.put(TENANT_ID_MDC_KEY, tenantId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && StringUtils.hasText(authentication.getName())) {
            MDC.put(USER_MDC_KEY, authentication.getName());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(TENANT_ID_MDC_KEY);
            MDC.remove(USER_MDC_KEY);
        }
    }
}
