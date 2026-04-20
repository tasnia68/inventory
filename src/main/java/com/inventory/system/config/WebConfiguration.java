package com.inventory.system.config;

import com.inventory.system.config.filter.RequestCorrelationFilter;
import com.inventory.system.config.tenant.TenantContextFilter;
import com.inventory.system.security.JwtService;
import com.inventory.system.service.StorefrontDomainService;
import jakarta.persistence.EntityManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties(AppCorsProperties.class)
public class WebConfiguration {

    @Bean
    public FilterRegistrationBean<CorsFilter> storefrontCorsFilter(CorsConfigurationSource corsConfigurationSource) {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorsFilter(corsConfigurationSource));
        registrationBean.addUrlPatterns(
                "/api/v1/storefront/public/*",
                "/api/v1/storefront/domains/caddy/validate"
        );
        registrationBean.setOrder(-102);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(
            EntityManager entityManager,
            StorefrontDomainService storefrontDomainService,
            JwtService jwtService) {
        FilterRegistrationBean<TenantContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantContextFilter(entityManager, storefrontDomainService, jwtService));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(-101);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter() {
        FilterRegistrationBean<RequestCorrelationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestCorrelationFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
