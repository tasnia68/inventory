package com.inventory.system.controller;

import com.inventory.system.common.entity.Role;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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

    @org.springframework.web.bind.annotation.PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Role>> createRole(
            @org.springframework.web.bind.annotation.RequestBody Role role) {
        Role savedRole = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Role>builder()
                .status(HttpStatus.CREATED.value())
                .message("Role created successfully")
                .data(savedRole)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
