package com.inventory.system.config;

import com.inventory.system.security.CustomAccessDeniedHandler;
import com.inventory.system.security.JwtAuthenticationEntryPoint;
import com.inventory.system.security.JwtAuthenticationFilter;
import com.inventory.system.config.tenant.TenantContextFilter;
import com.inventory.system.service.StorefrontDomainService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AppCorsProperties.class)
public class SecurityConfiguration {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
        private final AppCorsProperties appCorsProperties;
        private final StorefrontDomainService storefrontDomainService;

        public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        CustomAccessDeniedHandler customAccessDeniedHandler,
                        AppCorsProperties appCorsProperties,
                        StorefrontDomainService storefrontDomainService) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
                this.customAccessDeniedHandler = customAccessDeniedHandler;
                this.appCorsProperties = appCorsProperties;
                this.storefrontDomainService = storefrontDomainService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(customAccessDeniedHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/auth/**"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/tenants/register"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/storefront/public/**"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/storefront/domains/caddy/validate"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/product-images/*/file"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/users/invite/accept"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/webhooks/**"),
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/error"))
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                return request -> {
                        CorsConfiguration configuration = baseCorsConfiguration();
                        String origin = request.getHeader("Origin");

                        if (isPublicStorefrontRequest(request) && StringUtils.hasText(origin)) {
                                if (isStaticallyAllowed(origin) || isAllowedStorefrontOrigin(request, origin)) {
                                        configuration.setAllowedOrigins(List.of(origin));
                                        return configuration;
                                }
                        }

                        configuration.setAllowedOrigins(appCorsProperties.getAllowedOrigins());
                        return configuration;
                };
        }

        private CorsConfiguration baseCorsConfiguration() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID", "X-Request-ID", "X-Storefront-Host"));
                configuration.setExposedHeaders(List.of("X-Request-ID"));
                configuration.setAllowCredentials(true);
                return configuration;
        }

        private boolean isPublicStorefrontRequest(HttpServletRequest request) {
                String requestUri = request.getRequestURI();
                return requestUri != null && requestUri.startsWith("/api/v1/storefront/public/");
        }

        private boolean isStaticallyAllowed(String origin) {
                return appCorsProperties.getAllowedOrigins().contains(origin);
        }

        private boolean isAllowedStorefrontOrigin(HttpServletRequest request, String origin) {
                Optional<String> originTenantId = storefrontDomainService.resolveTenantIdForOrigin(origin);
                if (originTenantId.isEmpty()) {
                        return false;
                }

                String requestHost = request.getServerName();
                if (storefrontDomainService.isLocalDevelopmentHost(requestHost)) {
                        String override = request.getHeader(TenantContextFilter.STOREFRONT_HOST_OVERRIDE_HEADER);
                        if (!StringUtils.hasText(override)) {
                                return true;
                        }
                }

                String requestedStorefrontHost = resolveRequestedStorefrontHost(request);
                Optional<String> requestedTenantId = storefrontDomainService.resolveTenantIdForHost(requestedStorefrontHost);
                return requestedTenantId.isPresent() && requestedTenantId.get().equals(originTenantId.get());
        }

        private String resolveRequestedStorefrontHost(HttpServletRequest request) {
                String requestHost = request.getServerName();
                if (storefrontDomainService.isLocalDevelopmentHost(requestHost)) {
                        String override = request.getHeader(TenantContextFilter.STOREFRONT_HOST_OVERRIDE_HEADER);
                        if (StringUtils.hasText(override)) {
                                return override;
                        }
                }
                return requestHost;
        }
}
