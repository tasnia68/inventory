package com.inventory.system.service.impl;

import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.ShippingRateCard;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ShippingRateCardDto;
import com.inventory.system.payload.ShippingRateCardRequest;
import com.inventory.system.repository.CourierProfileRepository;
import com.inventory.system.repository.ShippingRateCardRepository;
import com.inventory.system.service.ShippingRateCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingRateCardServiceImpl implements ShippingRateCardService {

    private final ShippingRateCardRepository repository;
    private final CourierProfileRepository courierProfileRepository;

    @Override
    @Transactional
    public ShippingRateCardDto create(UUID courierProfileId, ShippingRateCardRequest request) {
        CourierProfile profile = courierProfileRepository.findById(courierProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("CourierProfile", "id", courierProfileId));
        ShippingRateCard card = new ShippingRateCard();
        card.setCourierProfile(profile);
        applyRequest(card, request);
        return toDto(repository.save(card));
    }

    @Override
    @Transactional
    public ShippingRateCardDto update(UUID id, ShippingRateCardRequest request) {
        ShippingRateCard card = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShippingRateCard", "id", id));
        applyRequest(card, request);
        return toDto(repository.save(card));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingRateCardDto> listByProfile(UUID courierProfileId) {
        return repository.findByCourierProfileId(courierProfileId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShippingRateCardDto> findByProfileAndZone(UUID courierProfileId, DeliveryZone zone) {
        return repository.findFirstByCourierProfileIdAndZone(courierProfileId, zone).map(this::toDto);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("ShippingRateCard", "id", id);
        }
        repository.deleteById(id);
    }

    private void applyRequest(ShippingRateCard card, ShippingRateCardRequest request) {
        card.setZone(request.getZone());
        card.setCustomerCharge(request.getCustomerCharge());
        card.setCourierCost(request.getCourierCost());
        card.setCodFeePercent(request.getCodFeePercent() != null ? request.getCodFeePercent() : BigDecimal.ZERO);
        card.setWeightKgIncluded(request.getWeightKgIncluded());
        card.setPerKgOverage(request.getPerKgOverage());
        card.setEffectiveFrom(request.getEffectiveFrom());
        card.setEffectiveTo(request.getEffectiveTo());
    }

    private ShippingRateCardDto toDto(ShippingRateCard card) {
        return new ShippingRateCardDto(
                card.getId(),
                card.getCourierProfile() != null ? card.getCourierProfile().getId() : null,
                card.getZone(),
                card.getCustomerCharge(),
                card.getCourierCost(),
                card.getCodFeePercent(),
                card.getWeightKgIncluded(),
                card.getPerKgOverage(),
                card.getEffectiveFrom(),
                card.getEffectiveTo()
        );
    }
}
