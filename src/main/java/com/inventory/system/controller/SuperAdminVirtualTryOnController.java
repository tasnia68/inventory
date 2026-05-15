package com.inventory.system.controller;

import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.entity.TenantVirtualTryOnSettings;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.service.VirtualTryOnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/virtual-try-on")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminVirtualTryOnController {

    private static final String PLATFORM_GEMINI_KEY_SETTING = "platform.gemini_api_key";
    private static final String PLATFORM_GEMINI_MODEL_SETTING = "platform.gemini_model";
    private static final String DEFAULT_GEMINI_MODEL = "gemini-3.1-flash-image-preview";

    private final VirtualTryOnService virtualTryOnService;
    private final TenantRepository tenantRepository;
    private final TenantSettingRepository tenantSettingRepository;

    public record TenantEntitlementRow(
            String tenantId, String tenantName, String subdomain,
            boolean enabled, int maxPerCustomerPerDay, int maxPerTenantPerMonth) {}

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<TenantEntitlementRow>>> listTenants() {
        List<TenantEntitlementRow> rows = new ArrayList<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            String tenantId = tenant.getId().toString();
            TenantVirtualTryOnSettings settings = virtualTryOnService.getTenantSettings(tenantId).orElse(null);
            rows.add(new TenantEntitlementRow(
                    tenantId, tenant.getName(), tenant.getSubdomain(),
                    settings != null && settings.isEnabled(),
                    settings != null ? settings.getMaxPerCustomerPerDay() : 3,
                    settings != null ? settings.getMaxPerTenantPerMonth() : 500
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<ApiResponse<TenantEntitlementRow>> updateTenant(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        int maxPerCustomerPerDay = body.get("maxPerCustomerPerDay") != null
                ? Integer.parseInt(String.valueOf(body.get("maxPerCustomerPerDay"))) : 3;
        int maxPerTenantPerMonth = body.get("maxPerTenantPerMonth") != null
                ? Integer.parseInt(String.valueOf(body.get("maxPerTenantPerMonth"))) : 500;
        TenantVirtualTryOnSettings saved = virtualTryOnService.upsertTenantSettings(
                tenantId, enabled, maxPerCustomerPerDay, maxPerTenantPerMonth);
        Tenant tenant = tenantRepository.findById(java.util.UUID.fromString(tenantId)).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(new TenantEntitlementRow(
                tenantId,
                tenant != null ? tenant.getName() : "",
                tenant != null ? tenant.getSubdomain() : "",
                saved.isEnabled(), saved.getMaxPerCustomerPerDay(), saved.getMaxPerTenantPerMonth()
        ), "Tenant entitlement updated"));
    }

    @GetMapping("/api-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiKeyStatus() {
        boolean configured = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_KEY_SETTING)
                .map(v -> v != null && !v.isBlank())
                .orElse(false);
        String model = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_MODEL_SETTING)
                .orElse(DEFAULT_GEMINI_MODEL);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "configured", configured,
                "model", model,
                "defaultModel", DEFAULT_GEMINI_MODEL
        )));
    }

    @PutMapping("/api-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setApiKey(@RequestBody Map<String, Object> body) {
        if (body.containsKey("apiKey")) {
            String value = body.get("apiKey") != null ? String.valueOf(body.get("apiKey")) : "";
            saveSetting(PLATFORM_GEMINI_KEY_SETTING, value);
        }
        if (body.containsKey("model")) {
            String value = body.get("model") != null ? String.valueOf(body.get("model")) : "";
            saveSetting(PLATFORM_GEMINI_MODEL_SETTING, value);
        }
        boolean configured = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_KEY_SETTING)
                .map(v -> v != null && !v.isBlank())
                .orElse(false);
        String model = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_MODEL_SETTING)
                .orElse(DEFAULT_GEMINI_MODEL);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("configured", configured, "model", model),
                "Settings updated"));
    }

    private void saveSetting(String key, String value) {
        TenantSetting setting = tenantSettingRepository.findBySettingKey(key)
                .orElseGet(TenantSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setSettingType("STRING");
        setting.setCategory("PLATFORM");
        tenantSettingRepository.save(setting);
    }
}
