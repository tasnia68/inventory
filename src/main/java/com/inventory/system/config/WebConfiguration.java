package com.inventory.system.config;

import com.inventory.system.config.filter.RequestCorrelationFilter;
import com.inventory.system.config.tenant.TenantContextFilter;
import jakarta.persistence.EntityManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppCorsProperties.class)
public class WebConfiguration {

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(EntityManager entityManager) {
        FilterRegistrationBean<TenantContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantContextFilter(entityManager));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // Set order if multiple filters exist
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
