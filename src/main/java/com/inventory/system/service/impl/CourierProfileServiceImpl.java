package com.inventory.system.service.impl;

import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CourierProfileDto;
import com.inventory.system.payload.CourierProfileRequest;
import com.inventory.system.repository.CourierProfileRepository;
import com.inventory.system.service.CourierProfileService;
import com.inventory.system.service.courier.CourierProvider;
import com.inventory.system.service.courier.CourierProviderException;
import com.inventory.system.service.courier.CourierProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourierProfileServiceImpl implements CourierProfileService {

    private final CourierProfileRepository repository;
    private final CourierProviderRegistry registry;

    @Override
    @Transactional
    public CourierProfileDto createProfile(CourierProfileRequest request) {
        validateProviderCode(request.getProviderCode());
        CourierProfile profile = new CourierProfile();
        applyRequest(profile, request);
        enforceDefaultInvariant(profile);
        return toDto(repository.save(profile));
    }

    @Override
    @Transactional
    public CourierProfileDto updateProfile(UUID id, CourierProfileRequest request) {
        CourierProfile profile = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourierProfile", "id", id));
        if (request.getProviderCode() != null && !request.getProviderCode().equalsIgnoreCase(profile.getProviderCode())) {
            throw new BadRequestException("providerCode cannot be changed; delete and recreate the profile instead");
        }
        applyRequest(profile, request);
        enforceDefaultInvariant(profile);
        return toDto(repository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public CourierProfileDto getProfile(UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CourierProfile", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierProfileDto> listProfiles() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteProfile(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("CourierProfile", "id", id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID id) {
        CourierProfile profile = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourierProfile", "id", id));
        CourierProvider provider = registry.resolve(profile.getProviderCode());
        try {
            return provider.getBalance(profile);
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public List<String> listRegisteredProviderCodes() {
        return registry.listProviderCodes();
    }

    private void applyRequest(CourierProfile profile, CourierProfileRequest request) {
        profile.setProviderCode(request.getProviderCode().toUpperCase());
        profile.setDisplayName(request.getDisplayName());
        profile.setDefault(request.isDefault());
        profile.setActive(request.isActive());
        profile.setCredentialsJson(request.getCredentialsJson());
        profile.setConfigJson(request.getConfigJson());
    }

    private void enforceDefaultInvariant(CourierProfile profile) {
        if (!profile.isDefault()) return;
        repository.findAll().stream()
                .filter(existing -> !existing.equals(profile))
                .filter(existing -> existing.isDefault())
                .forEach(existing -> {
                    existing.setDefault(false);
                    repository.save(existing);
                });
    }

    private void validateProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new BadRequestException("providerCode is required");
        }
        try {
            registry.resolve(providerCode);
        } catch (CourierProviderException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private CourierProfileDto toDto(CourierProfile profile) {
        return new CourierProfileDto(
                profile.getId(),
                profile.getProviderCode(),
                profile.getDisplayName(),
                profile.isDefault(),
                profile.isActive(),
                profile.getCredentialsJson(),
                profile.getConfigJson(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
