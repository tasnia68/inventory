package com.inventory.system.controller;

import com.inventory.system.common.entity.Permission;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.repository.PermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionRepository permissionRepository;

    public PermissionController(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.<List<Permission>>builder()
                .status(HttpStatus.OK.value())
                .message("Permissions retrieved successfully")
                .data(permissions)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
