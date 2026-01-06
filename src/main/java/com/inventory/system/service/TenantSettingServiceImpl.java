package com.inventory.system.service;

import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.exception.ResourceNotFoundException;
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
        List<TenantSetting> settings;
        if (category != null && !category.isBlank()) {
            settings = tenantSettingRepository.findByCategory(category);
        } else {
            settings = tenantSettingRepository.findAll();
        }
        return settings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSettingDto getSetting(String key) {
        TenantSetting setting = tenantSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        return mapToDto(setting);
    }

    @Override
    @Transactional
    public TenantSettingDto updateSetting(String key, String value, String type, String category) {
        TenantSetting setting = tenantSettingRepository.findBySettingKey(key)
                .orElse(new TenantSetting());

        if (setting.getId() == null) {
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
