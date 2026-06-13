package com.inventory.system.service;

import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.AuthResponse;
import com.inventory.system.payload.LoginRequest;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        tenantContextMock = Mockito.mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void authenticateRejectsUnknownWorkspace() {
        LoginRequest request = new LoginRequest();
        request.setWorkspace("missing-workspace");
        request.setEmail("admin@test.com");
        request.setPassword("secret");

        when(tenantRepository.findBySubdomainIgnoreCase("missing-workspace")).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.authenticate(request));

        assertEquals("Unknown workspace: missing-workspace", exception.getMessage());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void authenticateGeneratesTokenWithResolvedTenantUuid() {
        LoginRequest request = new LoginRequest();
        request.setWorkspace("default-tenant");
        request.setEmail("admin@test.com");
        request.setPassword("secret");

        UUID tenantUuid = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantUuid);
        tenant.setSubdomain("default-tenant");

        UserDetails userDetails = mock(UserDetails.class);

        when(tenantRepository.findBySubdomainIgnoreCase("default-tenant")).thenReturn(Optional.of(tenant));
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("secret", userDetails.getPassword())).thenReturn(true);
        when(jwtService.generateToken(userDetails, tenantUuid.toString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        tenantContextMock.when(() -> TenantContext.getTenantId()).thenReturn(tenantUuid.toString());

        AuthResponse response = authService.authenticate(request);

        assertEquals(tenantUuid.toString(), response.getTenantId());
        assertEquals("default-tenant", response.getTenantSubdomain());
        assertEquals("access-token", response.getAccessToken());
        verify(jwtService).generateToken(userDetails, tenantUuid.toString());
    }

    @Test
    void authenticateRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setWorkspace("default-tenant");
        request.setEmail("admin@test.com");
        request.setPassword("wrong-password");

        UUID tenantUuid = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantUuid);
        tenant.setSubdomain("default-tenant");

        UserDetails userDetails = mock(UserDetails.class);

        when(tenantRepository.findBySubdomainIgnoreCase("default-tenant")).thenReturn(Optional.of(tenant));
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("wrong-password", userDetails.getPassword())).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.authenticate(request));

        assertEquals("Bad credentials", exception.getMessage());
    }
}