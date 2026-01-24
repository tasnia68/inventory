package com.inventory.system.config;

import com.inventory.system.common.entity.Role;
import com.inventory.system.common.entity.User;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final com.inventory.system.repository.PermissionRepository permissionRepository;
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

    private static final Set<String> PERMISSIONS = Set.of(
            "MENU:DASHBOARD",
            "MENU:USER_MANAGEMENT",
            "MENU:CATALOG",
            "MENU:INVENTORY_CORE",
            "MENU:ADVANCED_INVENTORY",
            "MENU:PROCUREMENT",
            "MENU:SALES",
            "MENU:ANALYTICS",
            "MENU:SETTINGS");

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

        // Check if admin user already exists
        Optional<User> existingUser = userRepository.findByEmail(adminEmail);
        if (existingUser.isPresent()) {
            log.info("Bootstrap admin user already exists: {}", adminEmail);
            return;
        }

        // Create or get ADMIN role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("System Administrator");
                    role.setTenantId(tenantId);
                    role.setPermissions(allPermissions);
                    return roleRepository.save(role);
                });

        // Ensure admin role has all permissions (in case it existed but new permissions
        // added)
        if (adminRole.getPermissions() == null || adminRole.getPermissions().size() < allPermissions.size()) {
            adminRole.setPermissions(allPermissions);
            roleRepository.save(adminRole);
        }

        // Create bootstrap admin user
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName(adminFirstName);
        adminUser.setLastName(adminLastName);
        adminUser.setEnabled(true);
        adminUser.setTenantId(tenantId);
        adminUser.setRoles(Set.of(adminRole));

        userRepository.save(adminUser);
        log.info("Bootstrap admin user created successfully: {}", adminEmail);
        log.info("You can now login with email: {} and password from configuration", adminEmail);
    }

    private String formatDescription(String permName) {
        String[] parts = permName.split(":");
        if (parts.length > 1) {
            return parts[1].replace("_", " ") + " Access";
        }
        return permName;
    }
}
