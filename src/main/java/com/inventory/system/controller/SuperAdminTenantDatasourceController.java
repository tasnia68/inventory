package com.inventory.system.controller;

import com.inventory.system.config.tenant.routing.DedicatedDbProvisioningService;
import com.inventory.system.config.tenant.routing.TenantCutoverService;
import com.inventory.system.config.tenant.routing.TenantDatasourceAdminService;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.TenantDatasourceRequest;
import com.inventory.system.payload.TenantDatasourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Super-admin management of per-tenant dedicated databases. Gated by
 * {@code app.tenant.routing.enabled} (the underlying services only exist when
 * the feature is on). Credentials are write-only — never returned by any
 * endpoint here.
 */
@RestController
@RequestMapping("/api/v1/super-admin/tenants/{tenantId}/datasource")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class SuperAdminTenantDatasourceController {

    private final TenantDatasourceAdminService adminService;
    private final DedicatedDbProvisioningService provisioningService;
    private final TenantCutoverService cutoverService;

    @GetMapping
    public ResponseEntity<ApiResponse<TenantDatasourceResponse>> get(@PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.get(tenantId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<TenantDatasourceResponse>> upsert(
            @PathVariable String tenantId, @RequestBody TenantDatasourceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.upsert(tenantId, request), "Datasource configuration saved"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> test(@PathVariable String tenantId) {
        boolean ok = adminService.testConnection(tenantId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("ok", ok), ok ? "Connection succeeded" : "Connection failed"));
    }

    @PostMapping("/migrate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrate(@PathVariable String tenantId) {
        String version = provisioningService.migrateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("schemaVersion", version), "Dedicated DB migrated"));
    }

    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<TenantCutoverService.CutoverResult>> provision(
            @PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(
                cutoverService.cutover(tenantId), "Cutover complete"));
    }
}
