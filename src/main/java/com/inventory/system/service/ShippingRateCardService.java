package com.inventory.system.service;

import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.payload.ShippingRateCardDto;
import com.inventory.system.payload.ShippingRateCardRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingRateCardService {
    ShippingRateCardDto create(UUID courierProfileId, ShippingRateCardRequest request);
    ShippingRateCardDto update(UUID id, ShippingRateCardRequest request);
    List<ShippingRateCardDto> listByProfile(UUID courierProfileId);
    Optional<ShippingRateCardDto> findByProfileAndZone(UUID courierProfileId, DeliveryZone zone);
    void delete(UUID id);
}
