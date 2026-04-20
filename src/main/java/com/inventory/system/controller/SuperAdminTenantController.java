package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.SuperAdminTenantRequest;
import com.inventory.system.payload.SuperAdminTenantUpdateRequest;
import com.inventory.system.payload.TenantResponse;
import com.inventory.system.service.SuperAdminTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminTenantController {

    private final SuperAdminTenantService superAdminTenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
        return ResponseEntity.ok(ApiResponse.success(superAdminTenantService.getAllTenants()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody SuperAdminTenantRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(superAdminTenantService.createTenant(request), "Tenant created successfully"),
                HttpStatus.CREATED);
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SuperAdminTenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(superAdminTenantService.updateTenant(tenantId, request), "Tenant updated successfully"));
    }

    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<ApiResponse<TenantResponse>> activateTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(superAdminTenantService.activateTenant(tenantId), "Tenant activated successfully"));
    }

    @PostMapping("/{tenantId}/deactivate")
    public ResponseEntity<ApiResponse<TenantResponse>> deactivateTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(superAdminTenantService.deactivateTenant(tenantId), "Tenant deactivated successfully"));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID tenantId) {
        superAdminTenantService.deleteTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant removed successfully"));
    }
}
