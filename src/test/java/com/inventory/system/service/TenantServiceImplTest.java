package com.inventory.system.service;

import com.inventory.system.common.entity.Role;
import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.entity.User;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.TenantRequest;
import com.inventory.system.payload.TenantResponse;
import com.inventory.system.repository.RoleRepository;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TenantServiceImpl tenantService;

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
    void registerTenant_Success() {
        TenantRequest request = new TenantRequest();
        request.setName("Test Tenant");
        request.setSubdomain("test");
        request.setAdminEmail("admin@test.com");
        request.setAdminPassword("password");
        request.setPlan(Tenant.SubscriptionPlan.FREE);

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setSubscriptionPlan(request.getPlan());

        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);

        when(tenantRepository.existsBySubdomain(request.getSubdomain())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(filter.setParameter(anyString(), any())).thenReturn(filter);

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(request.getAdminPassword())).thenReturn("encodedPassword");

        TenantResponse response = tenantService.registerTenant(request);

        assertNotNull(response);
        assertEquals(tenant.getId(), response.getId());
        assertEquals("Test Tenant", response.getName());

        tenantContextMock.verify(() -> TenantContext.setTenantId(tenant.getId().toString()));
        verify(userRepository).save(any(User.class));
        verify(session).enableFilter("tenantFilter");
    }

    @Test
    void registerTenant_SubdomainExists() {
        TenantRequest request = new TenantRequest();
        request.setSubdomain("existing");

        when(tenantRepository.existsBySubdomain("existing")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> tenantService.registerTenant(request));
    }
}
