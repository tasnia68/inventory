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

    List<TenantSetting> findByCategory(String category);
}
