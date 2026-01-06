package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.TenantRequest;
import com.inventory.system.payload.TenantResponse;
import com.inventory.system.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TenantResponse>> registerTenant(@Valid @RequestBody TenantRequest request) {
        TenantResponse tenantResponse = tenantService.registerTenant(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Tenant registered successfully", tenantResponse), HttpStatus.CREATED);
    }
}
