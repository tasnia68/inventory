package com.inventory.system.service;

import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.repository.TenantSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantSettingServiceImpl implements TenantSettingService {

    private final TenantSettingRepository tenantSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TenantSettingDto> getSettings(String category) {
        String tenantId = TenantContext.getTenantId();
        List<TenantSetting> settings;
        if (category != null && !category.isBlank()) {
            settings = tenantSettingRepository.findByTenantIdAndCategoryOrderBySettingKeyAsc(tenantId, category);
        } else {
            settings = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(tenantId);
        }
        return settings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSettingDto getSetting(String key) {
        String tenantId = TenantContext.getTenantId();
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        return mapToDto(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<TenantSettingDto> findSetting(String key) {
        String tenantId = TenantContext.getTenantId();
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<TenantSettingDto> findSettingForTenant(String tenantId, String key) {
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public TenantSettingDto updateSetting(String key, String value, String type, String category) {
        return updateSettingForTenant(TenantContext.getTenantId(), key, value, type, category);
    }

    @Override
    @Transactional
    public TenantSettingDto updateSettingForTenant(String tenantId, String key, String value, String type, String category) {
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                .orElse(new TenantSetting());

        if (setting.getId() == null) {
            setting.setTenantId(tenantId);
            setting.setSettingKey(key);
        }

        setting.setSettingValue(value);
        setting.setSettingType(type != null ? type : "STRING");
        if (category != null) {
            setting.setCategory(category);
        }

        TenantSetting savedSetting = tenantSettingRepository.save(setting);
        return mapToDto(savedSetting);
    }

    @Override
    @Transactional
    public void updateSettings(List<TenantSettingDto> settings) {
        for (TenantSettingDto dto : settings) {
            updateSetting(dto.getKey(), dto.getValue(), dto.getType(), dto.getCategory());
        }
    }

    private TenantSettingDto mapToDto(TenantSetting setting) {
        return TenantSettingDto.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .type(setting.getSettingType())
                .category(setting.getCategory())
                .build();
    }
}
