package com.inventory.system.config;

import com.inventory.system.config.tenant.TenantContextFilter;
import jakarta.persistence.EntityManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfiguration {

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(EntityManager entityManager) {
        FilterRegistrationBean<TenantContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantContextFilter(entityManager));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // Set order if multiple filters exist
        return registrationBean;
    }
}
