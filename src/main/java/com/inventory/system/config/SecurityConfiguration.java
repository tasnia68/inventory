package com.inventory.system.config;

import com.inventory.system.security.CustomAccessDeniedHandler;
import com.inventory.system.security.JwtAuthenticationEntryPoint;
import com.inventory.system.security.JwtAuthenticationFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AppCorsProperties.class)
public class SecurityConfiguration {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
        private final AppCorsProperties appCorsProperties;

        public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        CustomAccessDeniedHandler customAccessDeniedHandler,
                        AppCorsProperties appCorsProperties) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
                this.customAccessDeniedHandler = customAccessDeniedHandler;
                this.appCorsProperties = appCorsProperties;
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
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(List.of("*"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("X-Request-ID"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
