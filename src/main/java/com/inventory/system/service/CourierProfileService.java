package com.inventory.system.service;

import com.inventory.system.payload.CourierProfileDto;
import com.inventory.system.payload.CourierProfileRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CourierProfileService {
    CourierProfileDto createProfile(CourierProfileRequest request);
    CourierProfileDto updateProfile(UUID id, CourierProfileRequest request);
    CourierProfileDto getProfile(UUID id);
    List<CourierProfileDto> listProfiles();
    void deleteProfile(UUID id);
    BigDecimal getBalance(UUID id);
    List<String> listRegisteredProviderCodes();
}
