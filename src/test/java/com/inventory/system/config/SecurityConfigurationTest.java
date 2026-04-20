package com.inventory.system.config;

import com.inventory.system.security.CustomAccessDeniedHandler;
import com.inventory.system.security.JwtAuthenticationEntryPoint;
import com.inventory.system.security.JwtAuthenticationFilter;
import com.inventory.system.service.StorefrontDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigurationTest {

    private SecurityConfiguration securityConfiguration;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        securityConfiguration = new SecurityConfiguration(
                mock(JwtAuthenticationFilter.class),
                mock(JwtAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class),
                new AppCorsProperties(),
                mock(StorefrontDomainService.class));
        passwordEncoder = securityConfiguration.passwordEncoder();
    }

    @Test
    void createsSecurityBeans() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(securityConfiguration.corsConfigurationSource()).isNotNull();
    }

    @Test
    void passwordEncoderWorks() {
        String password = "password";
        String encoded = passwordEncoder.encode(password);
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
    }
}
