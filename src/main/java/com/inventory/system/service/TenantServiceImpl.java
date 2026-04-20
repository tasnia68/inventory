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
import com.inventory.system.repository.PermissionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final String STOREFRONT_MODULE_ENABLED_KEY = "tenant.modules.storefront.enabled";
    private static final String STOREFRONT_CATEGORY = "STOREFRONT";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantSettingService tenantSettingService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public TenantResponse registerTenant(TenantRequest request) {
        if (tenantRepository.existsBySubdomainIgnoreCase(request.getSubdomain())) {
            throw new BadRequestException("Subdomain already exists");
        }

        boolean storefrontEnabled = Boolean.TRUE.equals(request.getStorefrontEnabled());

        // Create Tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setSubscriptionPlan(request.getPlan());
        tenant = tenantRepository.save(tenant);

        // Set Tenant Context for User and Role creation
        String tenantId = tenant.getId().toString();
        TenantContext.setTenantId(tenantId);

        try {
            // Enable Hibernate Filter for this tenant to ensure isolation
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);

            // Check/Create Admin Role for this Tenant
            // Roles are tenant-specific as they extend BaseEntity
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        role.setDescription("Administrator role for tenant " + tenantId);
                        role.setPermissions(new java.util.HashSet<>(permissionRepository.findAll()));
                        return roleRepository.save(role);
                    });

            if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
                adminRole.setPermissions(new java.util.HashSet<>(permissionRepository.findAll()));
                adminRole = roleRepository.save(adminRole);
            }

            // Create Admin User
            // Note: Since this is a new tenant, existsByEmail should be false in this context.
            // If we wanted to enforce global uniqueness for email (across all tenants), we would need a global check.
            // But per User entity, email is unique per tenant.
            if (userRepository.existsByEmail(request.getAdminEmail())) {
                 // Since context is set and filter enabled, this checks ONLY for this new tenant.
                 // Should be false.
            }

            User adminUser = new User();
            adminUser.setEmail(request.getAdminEmail());
            adminUser.setPassword(passwordEncoder.encode(request.getAdminPassword()));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEnabled(true);
            adminUser.setRoles(Collections.singleton(adminRole));

            userRepository.save(adminUser);
            tenantSettingService.updateSettingForTenant(
                    tenantId,
                    STOREFRONT_MODULE_ENABLED_KEY,
                    Boolean.toString(storefrontEnabled),
                    "BOOLEAN",
                    STOREFRONT_CATEGORY);

        } finally {
            TenantContext.clear();
        }

        return mapToResponse(tenant, storefrontEnabled);
    }

    private TenantResponse mapToResponse(Tenant tenant, boolean storefrontEnabled) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSubdomain(tenant.getSubdomain());
        response.setStorefrontEnabled(storefrontEnabled);
        response.setStatus(tenant.getStatus());
        response.setSubscriptionPlan(tenant.getSubscriptionPlan());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }
}
