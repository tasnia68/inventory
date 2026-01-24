package com.inventory.system.service;

import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.ReturnAuthorizationDto;
import com.inventory.system.payload.UpdateRmaStatusRequest;

import java.util.UUID;

public interface RmaService {
    ReturnAuthorizationDto createRma(CreateRmaRequest request);
    ReturnAuthorizationDto getRma(UUID id);
    ReturnAuthorizationDto updateRmaStatus(UUID id, UpdateRmaStatusRequest request);
}
