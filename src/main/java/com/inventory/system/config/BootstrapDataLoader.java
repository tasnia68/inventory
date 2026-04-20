package com.inventory.system.config;

import com.inventory.system.common.entity.Role;
import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.entity.User;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.RoleRepository;
import com.inventory.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapDataLoader implements CommandLineRunner {

    private static final String STOREFRONT_MODULE_ENABLED_KEY = "tenant.modules.storefront.enabled";
    private static final String STOREFRONT_CATEGORY = "STOREFRONT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final com.inventory.system.repository.PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:admin@test.com}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.firstName:System}")
    private String adminFirstName;

    @Value("${app.bootstrap.admin.lastName:Admin}")
    private String adminLastName;

    @Value("${app.bootstrap.admin.tenantId:default-tenant}")
    private String tenantId;

    @Value("${app.bootstrap.super-admin.email:superadmin@test.com}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin.password:SuperAdmin123!}")
    private String superAdminPassword;

    @Value("${app.bootstrap.super-admin.firstName:Platform}")
    private String superAdminFirstName;

    @Value("${app.bootstrap.super-admin.lastName:Admin}")
    private String superAdminLastName;

    @Value("${app.bootstrap.super-admin.tenantId:platform}")
    private String superAdminTenantId;

    private static final Set<String> PERMISSIONS = Set.of(
            "MENU:DASHBOARD",
            "MENU:USER_MANAGEMENT",
            "MENU:CATALOG",
            "MENU:INVENTORY_CORE",
            "MENU:ADVANCED_INVENTORY",
            "MENU:PROCUREMENT",
            "MENU:SALES",
            "MENU:ACCOUNTING",
            "MENU:PAYROLL",
            "MENU:ANALYTICS",
            "MENU:SETTINGS",
            "PAYROLL:EMPLOYEE_VIEW",
            "PAYROLL:EMPLOYEE_MANAGE",
            "PAYROLL:STRUCTURE_MANAGE",
            "PAYROLL:RUN_MANAGE",
            "PAYROLL:RUN_APPROVE",
            "PAYROLL:PAYSLIP_VIEW",
            "POS:MANUAL_DISCOUNT_OVERRIDE",
            "POS:CASH_CONTROL",
            "POS:SUSPEND_SALE",
            "POS:SETTLEMENT_APPROVE");

    @Override
    public void run(String... args) throws Exception {
        log.info("Running bootstrap data loader...");

        // Create standard permissions
        Set<com.inventory.system.common.entity.Permission> allPermissions = new java.util.HashSet<>();
        for (String permName : PERMISSIONS) {
            com.inventory.system.common.entity.Permission permission = permissionRepository.findByName(permName)
                    .orElseGet(() -> {
                        com.inventory.system.common.entity.Permission p = new com.inventory.system.common.entity.Permission();
                        p.setName(permName);
                        p.setDescription(formatDescription(permName));
                        return permissionRepository.save(p);
                    });
            allPermissions.add(permission);
        }
        log.info("Ensured {} permissions exist", allPermissions.size());

        Tenant superAdminTenant = ensureTenant(superAdminTenantId, "Platform");
        ensureSuperAdmin(superAdminTenant, allPermissions);

        Tenant bootstrapTenant = ensureBootstrapTenant();
        String bootstrapTenantId = bootstrapTenant.getId().toString();
        ensureBootstrapStorefrontModuleSetting(bootstrapTenantId);

        Role adminRole = roleRepository.findByNameAndTenantId("ROLE_ADMIN", bootstrapTenantId)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("System Administrator");
                    role.setTenantId(bootstrapTenantId);
                    role.setPermissions(allPermissions);
                    return roleRepository.save(role);
                });

        // Ensure admin role has all permissions (in case it existed but new permissions
        // added)
        if (adminRole.getPermissions() == null || adminRole.getPermissions().size() < allPermissions.size()) {
            adminRole.setPermissions(allPermissions);
            roleRepository.save(adminRole);
        }

        Optional<User> existingUser = userRepository.findByEmail(adminEmail);
        if (existingUser.isPresent()) {
            User adminUser = existingUser.get();
            boolean updated = false;

            if (!bootstrapTenantId.equals(adminUser.getTenantId())) {
                userRepository.updateTenantIdById(adminUser.getId(), bootstrapTenantId);
                adminUser.setTenantId(bootstrapTenantId);
                updated = true;
            }
            if (!adminUser.isEnabled()) {
                adminUser.setEnabled(true);
                updated = true;
            }
            if (!passwordEncoder.matches(adminPassword, adminUser.getPassword())) {
                adminUser.setPassword(passwordEncoder.encode(adminPassword));
                updated = true;
            }
            if (!adminFirstName.equals(adminUser.getFirstName())) {
                adminUser.setFirstName(adminFirstName);
                updated = true;
            }
            if (!adminLastName.equals(adminUser.getLastName())) {
                adminUser.setLastName(adminLastName);
                updated = true;
            }
            if (!adminUser.getRoles().contains(adminRole) || adminUser.getRoles().size() != 1) {
                adminUser.setRoles(Set.of(adminRole));
                updated = true;
            }

            if (updated) {
                userRepository.save(adminUser);
                log.info("Bootstrap admin user repaired for tenant {}: {}", bootstrapTenant.getSubdomain(), adminEmail);
            } else {
                log.info("Bootstrap admin user already exists: {}", adminEmail);
            }
            return;
        }

        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName(adminFirstName);
        adminUser.setLastName(adminLastName);
        adminUser.setEnabled(true);
        adminUser.setTenantId(bootstrapTenantId);
        adminUser.setRoles(Set.of(adminRole));

        userRepository.save(adminUser);
        log.info("Bootstrap admin user created successfully for tenant {}: {}", bootstrapTenant.getSubdomain(), adminEmail);
        log.info("You can now login with workspace: {}, email: {}, and password from configuration", bootstrapTenant.getSubdomain(), adminEmail);
    }

    private Tenant ensureBootstrapTenant() {
        return ensureTenant(tenantId, null);
    }

    private Tenant ensureTenant(String configuredIdentifier, String fallbackName) {
        String identifier = configuredIdentifier == null ? "" : configuredIdentifier.trim();
        if (identifier.isBlank()) {
            throw new IllegalStateException("Bootstrap tenantId must not be blank");
        }

        Optional<Tenant> existing = resolveTenant(identifier);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            UUID.fromString(identifier);
            throw new IllegalStateException("Configured bootstrap tenantId does not match an existing tenant: " + identifier);
        } catch (IllegalArgumentException ignored) {
            Tenant tenant = new Tenant();
            tenant.setName(fallbackName != null && !fallbackName.isBlank() ? fallbackName : buildBootstrapTenantName(identifier));
            tenant.setSubdomain(identifier.toLowerCase());
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setSubscriptionPlan(Tenant.SubscriptionPlan.ENTERPRISE);
            Tenant savedTenant = tenantRepository.save(tenant);
            log.info("Bootstrap tenant created successfully: {} ({})", savedTenant.getSubdomain(), savedTenant.getId());
            return savedTenant;
        }
    }

    private Optional<Tenant> resolveTenant(String identifier) {
        Optional<Tenant> bySubdomain = tenantRepository.findBySubdomainIgnoreCase(identifier);
        if (bySubdomain.isPresent()) {
            return bySubdomain;
        }

        try {
            return tenantRepository.findById(UUID.fromString(identifier));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void ensureBootstrapStorefrontModuleSetting(String bootstrapTenantId) {
        tenantSettingRepository.findByTenantIdAndSettingKey(bootstrapTenantId, STOREFRONT_MODULE_ENABLED_KEY)
                .orElseGet(() -> {
                    TenantSetting setting = new TenantSetting();
                    setting.setTenantId(bootstrapTenantId);
                    setting.setSettingKey(STOREFRONT_MODULE_ENABLED_KEY);
                    setting.setSettingValue("true");
                    setting.setSettingType("BOOLEAN");
                    setting.setCategory(STOREFRONT_CATEGORY);
                    TenantSetting savedSetting = tenantSettingRepository.save(setting);
                    log.info("Enabled storefront module for bootstrap tenant {}", bootstrapTenantId);
                    return savedSetting;
                });
    }

    private String buildBootstrapTenantName(String identifier) {
        String normalized = identifier.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return "Default Tenant";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }

        return builder.isEmpty() ? "Default Tenant" : builder.toString();
    }

    private void ensureSuperAdmin(Tenant superAdminTenant, Set<com.inventory.system.common.entity.Permission> allPermissions) {
        String superAdminTenantIdValue = superAdminTenant.getId().toString();
        Optional<User> existingUser = userRepository.findByEmail(superAdminEmail);
        if (existingUser.isPresent()) {
            User superAdminUser = existingUser.get();
            boolean updated = false;

            if (!superAdminTenantIdValue.equals(superAdminUser.getTenantId())) {
                userRepository.updateTenantIdById(superAdminUser.getId(), superAdminTenantIdValue);
                superAdminUser.setTenantId(superAdminTenantIdValue);
                updated = true;
            }
            if (!superAdminUser.isEnabled()) {
                superAdminUser.setEnabled(true);
                updated = true;
            }
            if (!passwordEncoder.matches(superAdminPassword, superAdminUser.getPassword())) {
                superAdminUser.setPassword(passwordEncoder.encode(superAdminPassword));
                updated = true;
            }
            if (!superAdminFirstName.equals(superAdminUser.getFirstName())) {
                superAdminUser.setFirstName(superAdminFirstName);
                updated = true;
            }
            if (!superAdminLastName.equals(superAdminUser.getLastName())) {
                superAdminUser.setLastName(superAdminLastName);
                updated = true;
            }

            Role superAdminRole = roleRepository.findByNameAndTenantId("ROLE_SUPER_ADMIN", superAdminTenantIdValue)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_SUPER_ADMIN");
                        role.setDescription("Platform super administrator");
                        role.setTenantId(superAdminTenantIdValue);
                        role.setPermissions(allPermissions);
                        return roleRepository.save(role);
                    });

            if (superAdminRole.getPermissions() == null || superAdminRole.getPermissions().size() < allPermissions.size()) {
                superAdminRole.setPermissions(allPermissions);
                roleRepository.save(superAdminRole);
            }

            if (!superAdminUser.getRoles().contains(superAdminRole) || superAdminUser.getRoles().size() != 1) {
                superAdminUser.setRoles(Set.of(superAdminRole));
                updated = true;
            }

            if (updated) {
                userRepository.save(superAdminUser);
                log.info("Bootstrap super admin repaired: {}", superAdminEmail);
            } else {
                log.info("Bootstrap super admin already exists: {}", superAdminEmail);
            }
            return;
        }

        Role superAdminRole = roleRepository.findByNameAndTenantId("ROLE_SUPER_ADMIN", superAdminTenantIdValue)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_SUPER_ADMIN");
                    role.setDescription("Platform super administrator");
                    role.setTenantId(superAdminTenantIdValue);
                    role.setPermissions(allPermissions);
                    return roleRepository.save(role);
                });

        if (superAdminRole.getPermissions() == null || superAdminRole.getPermissions().size() < allPermissions.size()) {
            superAdminRole.setPermissions(allPermissions);
            roleRepository.save(superAdminRole);
        }

        User superAdminUser = new User();
        superAdminUser.setEmail(superAdminEmail);
        superAdminUser.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdminUser.setFirstName(superAdminFirstName);
        superAdminUser.setLastName(superAdminLastName);
        superAdminUser.setEnabled(true);
        superAdminUser.setTenantId(superAdminTenantIdValue);
        superAdminUser.setRoles(Set.of(superAdminRole));

        userRepository.save(superAdminUser);
        log.info("Bootstrap super admin created successfully: {}", superAdminEmail);
    }

    private String formatDescription(String permName) {
        String[] parts = permName.split(":");
        if (parts.length > 1) {
            return parts[1].replace("_", " ") + " Access";
        }
        return permName;
    }
}
