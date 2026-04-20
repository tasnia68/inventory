package com.inventory.system.config;

import com.inventory.system.config.tenant.TenantContextFilter;
import com.inventory.system.security.CustomAccessDeniedHandler;
import com.inventory.system.security.JwtAuthenticationEntryPoint;
import com.inventory.system.security.JwtAuthenticationFilter;
import com.inventory.system.service.StorefrontDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigurationCorsTest {

    private static final String CUSTOM_ORIGIN = "http://shop.localdemo.test2:5174";

    private StorefrontDomainService storefrontDomainService;
    private SecurityConfiguration securityConfiguration;

    @BeforeEach
    void setUp() {
        storefrontDomainService = mock(StorefrontDomainService.class);
        securityConfiguration = new SecurityConfiguration(
                mock(JwtAuthenticationFilter.class),
                mock(JwtAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class),
                new AppCorsProperties(),
                storefrontDomainService);
    }

    @Test
    void allowsDynamicStorefrontOriginForMatchingTenant() {
        when(storefrontDomainService.resolveTenantIdForOrigin(CUSTOM_ORIGIN)).thenReturn(Optional.of("tenant-a"));
        when(storefrontDomainService.isLocalDevelopmentHost("localhost")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("shop.localdemo.test2")).thenReturn(Optional.of("tenant-a"));

        MockHttpServletRequest request = storefrontRequest();
        CorsConfiguration configuration = securityConfiguration.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).containsExactly(CUSTOM_ORIGIN);
    }

    @Test
    void allowsDynamicStorefrontOriginForLocalPreflightWithoutOverrideHeader() {
        when(storefrontDomainService.resolveTenantIdForOrigin(CUSTOM_ORIGIN)).thenReturn(Optional.of("tenant-a"));
        when(storefrontDomainService.isLocalDevelopmentHost("localhost")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/storefront/public/config");
        request.setServerName("localhost");
        request.addHeader("Origin", CUSTOM_ORIGIN);
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "content-type,x-storefront-host");

        CorsConfiguration configuration = securityConfiguration.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).containsExactly(CUSTOM_ORIGIN);
    }

    @Test
    void rejectsDynamicStorefrontOriginForDifferentTenant() {
        when(storefrontDomainService.resolveTenantIdForOrigin(CUSTOM_ORIGIN)).thenReturn(Optional.of("tenant-a"));
        when(storefrontDomainService.isLocalDevelopmentHost("localhost")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("shop.localdemo.test2")).thenReturn(Optional.of("tenant-b"));

        MockHttpServletRequest request = storefrontRequest();
        CorsConfiguration configuration = securityConfiguration.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).doesNotContain(CUSTOM_ORIGIN);
    }

    @Test
    void keepsStaticOriginsForBackofficeRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.setServerName("localhost");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration configuration = securityConfiguration.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).contains("http://localhost:5173");
    }

    private MockHttpServletRequest storefrontRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/storefront/public/products");
        request.setServerName("localhost");
        request.addHeader("Origin", CUSTOM_ORIGIN);
        request.addHeader(TenantContextFilter.STOREFRONT_HOST_OVERRIDE_HEADER, "shop.localdemo.test2");
        return request;
    }
}