package com.inventory.system.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigurationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void contextLoadsWithSecurityBeans() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void passwordEncoderWorks() {
        String password = "password";
        String encoded = passwordEncoder.encode(password);
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
    }
}
