package com.inventory.system.service;

import com.inventory.system.payload.TenantSettingDto;
import java.util.List;
import java.util.Map;

public interface TenantSettingService {
    List<TenantSettingDto> getSettings(String category);
    TenantSettingDto getSetting(String key);
    TenantSettingDto updateSetting(String key, String value, String type, String category);
    void updateSettings(List<TenantSettingDto> settings);
}
