package com.inventory.system.repository;

import com.inventory.system.common.entity.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSettingRepository extends JpaRepository<TenantSetting, UUID> {

    Optional<TenantSetting> findBySettingKey(String settingKey);

    Optional<TenantSetting> findByTenantIdAndSettingKey(String tenantId, String settingKey);

    List<TenantSetting> findByCategory(String category);

    List<TenantSetting> findByTenantIdOrderBySettingKeyAsc(String tenantId);

    List<TenantSetting> findByTenantIdAndCategoryOrderBySettingKeyAsc(String tenantId, String category);

    /**
     * Native-query lookup that bypasses the Hibernate tenantFilter so platform-level
     * settings (e.g. shared Gemini API key) can be read while the request runs in
     * a different tenant's context.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT setting_value FROM tenant_settings WHERE setting_key = ?1 LIMIT 1",
            nativeQuery = true)
    Optional<String> findValueAcrossTenantsBySettingKey(String settingKey);
}
