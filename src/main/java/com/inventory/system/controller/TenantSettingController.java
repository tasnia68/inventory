package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.service.TenantSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class TenantSettingController {

    private final TenantSettingService tenantSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantSettingDto>>> getSettings(
            @RequestParam(required = false) String category) {
        List<TenantSettingDto> settings = tenantSettingService.getSettings(category);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<TenantSettingDto>> getSetting(@PathVariable String key) {
        TenantSettingDto setting = tenantSettingService.getSetting(key);
        return ResponseEntity.ok(ApiResponse.success(setting));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody List<TenantSettingDto> settings) {
        tenantSettingService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.success(null, "Settings updated successfully"));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<TenantSettingDto>> updateSetting(
            @PathVariable String key,
            @RequestBody TenantSettingDto request) {
        TenantSettingDto updatedSetting = tenantSettingService.updateSetting(
                key, request.getValue(), request.getType(), request.getCategory());
        return ResponseEntity.ok(ApiResponse.success(updatedSetting, "Setting updated successfully"));
    }
}
