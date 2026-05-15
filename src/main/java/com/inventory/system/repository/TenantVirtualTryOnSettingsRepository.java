package com.inventory.system.repository;

import com.inventory.system.common.entity.TenantVirtualTryOnSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantVirtualTryOnSettingsRepository extends JpaRepository<TenantVirtualTryOnSettings, String> {
}
