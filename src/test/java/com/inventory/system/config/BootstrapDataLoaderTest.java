package com.inventory.system.config;

import com.inventory.system.common.entity.Permission;
import com.inventory.system.common.entity.Role;
import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.entity.User;
import com.inventory.system.repository.PermissionRepository;
import com.inventory.system.repository.RoleRepository;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapDataLoaderTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantSettingRepository tenantSettingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BootstrapDataLoader bootstrapDataLoader;

    @Test
    void runRepairsBootstrapAdminToRealTenantAndEnablesStorefront() throws Exception {
        ReflectionTestUtils.setField(bootstrapDataLoader, "adminEmail", "admin@test.com");
        ReflectionTestUtils.setField(bootstrapDataLoader, "adminPassword", "Admin123!");
        ReflectionTestUtils.setField(bootstrapDataLoader, "adminFirstName", "System");
        ReflectionTestUtils.setField(bootstrapDataLoader, "adminLastName", "Admin");
        ReflectionTestUtils.setField(bootstrapDataLoader, "tenantId", "default-tenant");
        ReflectionTestUtils.setField(bootstrapDataLoader, "superAdminEmail", "superadmin@test.com");
        ReflectionTestUtils.setField(bootstrapDataLoader, "superAdminPassword", "SuperAdmin123!");
        ReflectionTestUtils.setField(bootstrapDataLoader, "superAdminFirstName", "Platform");
        ReflectionTestUtils.setField(bootstrapDataLoader, "superAdminLastName", "Admin");
        ReflectionTestUtils.setField(bootstrapDataLoader, "superAdminTenantId", "platform");

        UUID platformTenantUuid = UUID.randomUUID();
        UUID tenantUuid = UUID.randomUUID();
        Tenant platformTenant = new Tenant();
        platformTenant.setId(platformTenantUuid);
        platformTenant.setName("Platform");
        platformTenant.setSubdomain("platform");

        Tenant savedTenant = new Tenant();
        savedTenant.setId(tenantUuid);
        savedTenant.setName("Default Tenant");
        savedTenant.setSubdomain("default-tenant");

        User superAdmin = new User();
        superAdmin.setEmail("superadmin@test.com");
        superAdmin.setPassword("encoded-super-admin");
        superAdmin.setEnabled(true);
        superAdmin.setTenantId("platform");

        User existingAdmin = new User();
        existingAdmin.setEmail("admin@test.com");
        existingAdmin.setTenantId("default-tenant");
        existingAdmin.setPassword("encoded-admin");
        existingAdmin.setFirstName("System");
        existingAdmin.setLastName("Admin");
        existingAdmin.setEnabled(true);
        Role legacyRole = new Role();
        legacyRole.setName("ROLE_ADMIN");
        legacyRole.setTenantId("default-tenant");
        existingAdmin.setRoles(Set.of(legacyRole));

        Role superAdminRole = new Role();
        superAdminRole.setName("ROLE_SUPER_ADMIN");
        superAdminRole.setTenantId("platform");
        superAdmin.setRoles(Set.of(superAdminRole));

        when(permissionRepository.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class);
            return Optional.of(new Permission(null, name, name));
        });
        when(userRepository.findByEmail("superadmin@test.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(existingAdmin));
        when(tenantRepository.findBySubdomainIgnoreCase("platform")).thenReturn(Optional.empty());
        when(tenantRepository.findBySubdomainIgnoreCase("default-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            if ("platform".equals(tenant.getSubdomain())) {
                return platformTenant;
            }
            return savedTenant;
        });
        when(roleRepository.findByNameAndTenantId("ROLE_ADMIN", tenantUuid.toString())).thenReturn(Optional.empty());
        when(roleRepository.findByNameAndTenantId("ROLE_SUPER_ADMIN", platformTenantUuid.toString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantSettingRepository.findByTenantIdAndSettingKey(tenantUuid.toString(), "tenant.modules.storefront.enabled"))
                .thenReturn(Optional.empty());
        when(tenantSettingRepository.save(any(TenantSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.updateTenantIdById(eq(superAdmin.getId()), eq(platformTenantUuid.toString()))).thenReturn(1);
        when(userRepository.updateTenantIdById(existingAdmin.getId(), tenantUuid.toString())).thenReturn(1);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.matches("SuperAdmin123!", "encoded-super-admin")).thenReturn(true);
        when(passwordEncoder.matches("Admin123!", "encoded-admin")).thenReturn(true);

        bootstrapDataLoader.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User repairedAdmin = userCaptor.getAllValues().stream()
            .filter(user -> "admin@test.com".equals(user.getEmail()))
            .findFirst()
            .orElseThrow();
        User repairedSuperAdmin = userCaptor.getAllValues().stream()
            .filter(user -> "superadmin@test.com".equals(user.getEmail()))
            .findFirst()
            .orElseThrow();
        assertEquals(tenantUuid.toString(), repairedAdmin.getTenantId());
        assertTrue(repairedAdmin.isEnabled());
        assertEquals(1, repairedAdmin.getRoles().size());
        assertEquals(tenantUuid.toString(), repairedAdmin.getRoles().iterator().next().getTenantId());
        assertEquals(platformTenantUuid.toString(), repairedSuperAdmin.getTenantId());
        assertTrue(repairedSuperAdmin.isEnabled());
        assertEquals(1, repairedSuperAdmin.getRoles().size());
        assertEquals(platformTenantUuid.toString(), repairedSuperAdmin.getRoles().iterator().next().getTenantId());

        ArgumentCaptor<TenantSetting> settingCaptor = ArgumentCaptor.forClass(TenantSetting.class);
        verify(tenantSettingRepository).save(settingCaptor.capture());
        TenantSetting setting = settingCaptor.getValue();
        assertEquals(tenantUuid.toString(), setting.getTenantId());
        assertEquals("tenant.modules.storefront.enabled", setting.getSettingKey());
        assertEquals("true", setting.getSettingValue());
        assertEquals("BOOLEAN", setting.getSettingType());

        verify(roleRepository).findByNameAndTenantId(eq("ROLE_ADMIN"), eq(tenantUuid.toString()));
        verify(roleRepository).findByNameAndTenantId(eq("ROLE_SUPER_ADMIN"), eq(platformTenantUuid.toString()));
        verify(userRepository).updateTenantIdById(eq(superAdmin.getId()), eq(platformTenantUuid.toString()));
        verify(userRepository).updateTenantIdById(eq(existingAdmin.getId()), eq(tenantUuid.toString()));
    }
}