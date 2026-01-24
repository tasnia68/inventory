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

    @Override
    public void run(String... args) throws Exception {
        log.info("Running bootstrap data loader...");

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
                    return roleRepository.save(role);
                });

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
}
