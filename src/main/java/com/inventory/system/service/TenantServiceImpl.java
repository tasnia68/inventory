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
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public TenantResponse registerTenant(TenantRequest request) {
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BadRequestException("Subdomain already exists");
        }

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
                        return roleRepository.save(role);
                    });

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

        } finally {
            TenantContext.clear();
        }

        return mapToResponse(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSubdomain(tenant.getSubdomain());
        response.setStatus(tenant.getStatus());
        response.setSubscriptionPlan(tenant.getSubscriptionPlan());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }
}
