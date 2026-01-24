package com.inventory.system.controller;

import com.inventory.system.common.entity.Permission;
import com.inventory.system.common.entity.Role;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateRoleRequest;
import com.inventory.system.repository.PermissionRepository;
import com.inventory.system.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.<List<Role>>builder()
                .status(HttpStatus.OK.value())
                .message("Roles retrieved successfully")
                .data(roles)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody CreateRoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setTenantId(com.inventory.system.config.tenant.TenantContext.getTenantId());

        if (request.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String permName : request.getPermissions()) {
                permissionRepository.findByName(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Role>builder()
                .status(HttpStatus.CREATED.value())
                .message("Role created successfully")
                .data(savedRole)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable UUID id, @RequestBody CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String permName : request.getPermissions()) {
                permissionRepository.findByName(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        return ResponseEntity.ok(ApiResponse.<Role>builder()
                .status(HttpStatus.OK.value())
                .message("Role updated successfully")
                .data(savedRole)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
