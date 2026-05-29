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

    /**
     * Tenant-scoped native lookup that bypasses the Hibernate {@code tenantFilter}.
     * Required for super-admin code paths that need to read a specific tenant's
     * setting while the request runs in a different tenant's context (e.g.
     * platform admin listing storefront-enabled flags for every tenant).
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT setting_value FROM tenant_settings "
                  + "WHERE tenant_id = ?1 AND setting_key = ?2 LIMIT 1",
            nativeQuery = true)
    Optional<String> findValueByTenantIdAndSettingKey(String tenantId, String settingKey);

    /**
     * Tenant-scoped entity lookup that bypasses the Hibernate {@code tenantFilter}.
     * Used by super-admin upsert paths so an existing row is found (and updated)
     * even when the request runs in a different tenant's context — otherwise the
     * filtered finder returns empty and the upsert would attempt an INSERT that
     * collides with the {@code (tenant_id, setting_key)} unique constraint.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT * FROM tenant_settings "
                  + "WHERE tenant_id = ?1 AND setting_key = ?2 LIMIT 1",
            nativeQuery = true)
    Optional<TenantSetting> findEntityByTenantIdAndSettingKey(String tenantId, String settingKey);
}
