package com.inventory.system.security;

import com.inventory.system.common.entity.User;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void loadUserByUsernameUsesActiveTenantId() {
        TenantContext.setTenantId("tenant-uuid");

        User user = new User();
        user.setEmail("admin@test.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setTenantId("tenant-uuid");

        when(userRepository.findByEmailAndTenantId("admin@test.com", "tenant-uuid")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@test.com");

        assertEquals("admin@test.com", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        verify(userRepository).findByEmailAndTenantId("admin@test.com", "tenant-uuid");
    }

    @Test
    void loadUserByUsernameFailsWithoutTenantContext() {
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("admin@test.com"));

        assertEquals("Tenant context is required for user lookup: admin@test.com", exception.getMessage());
    }
}