package com.inventory.system.service;

import com.inventory.system.common.entity.UnitOfMeasure;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.UnitOfMeasureDto;
import com.inventory.system.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {

    private final UnitOfMeasureRepository uomRepository;

    @Override
    @Transactional
    public UnitOfMeasureDto createUom(UnitOfMeasureDto dto) {
        if (Boolean.TRUE.equals(dto.getIsBase())) {
            validateNoExistingBase(dto.getCategory(), null);
            if (dto.getConversionFactor().compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Base unit must have conversion factor of 1.0");
            }
        }

        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setName(dto.getName());
        uom.setCode(dto.getCode());
        uom.setCategory(dto.getCategory());
        if (dto.getIsBase() != null) {
            uom.setIsBase(dto.getIsBase());
        } else {
            uom.setIsBase(false);
        }
        uom.setConversionFactor(dto.getConversionFactor());

        return mapToDto(uomRepository.save(uom));
    }

    @Override
    @Transactional
    public UnitOfMeasureDto updateUom(UUID id, UnitOfMeasureDto dto) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit of Measure", "id", id));

        if (Boolean.TRUE.equals(dto.getIsBase()) && !uom.getIsBase()) {
            validateNoExistingBase(dto.getCategory(), id);
        }

        if (Boolean.TRUE.equals(dto.getIsBase()) && dto.getConversionFactor().compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Base unit must have conversion factor of 1.0");
        }

        uom.setName(dto.getName());
        uom.setCode(dto.getCode());
        uom.setCategory(dto.getCategory());
        if (dto.getIsBase() != null) {
            uom.setIsBase(dto.getIsBase());
        }
        uom.setConversionFactor(dto.getConversionFactor());

        return mapToDto(uomRepository.save(uom));
    }

    @Override
    @Transactional
    public void deleteUom(UUID id) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit of Measure", "id", id));
        uomRepository.delete(uom);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitOfMeasureDto getUom(UUID id) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit of Measure", "id", id));
        return mapToDto(uom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitOfMeasureDto> getAllUoms() {
        return uomRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal value, UUID fromUomId, UUID toUomId) {
        UnitOfMeasure fromUom = uomRepository.findById(fromUomId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit of Measure", "id", fromUomId));
        UnitOfMeasure toUom = uomRepository.findById(toUomId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit of Measure", "id", toUomId));

        if (fromUom.getCategory() != toUom.getCategory()) {
            throw new IllegalArgumentException("Cannot convert between different UOM categories: "
                    + fromUom.getCategory() + " and " + toUom.getCategory());
        }

        if (fromUomId.equals(toUomId)) {
            return value;
        }

        // Logic:
        // Base = kg (factor 1)
        // From = g (factor 0.001) -> 1g = 0.001kg
        // To = kg (factor 1)
        // Value = 500
        // BaseValue = 500 * 0.001 = 0.5
        // Result = 0.5 / 1 = 0.5

        // From = lb (factor 0.453)
        // To = g (factor 0.001)
        // Value = 1
        // BaseValue = 1 * 0.453 = 0.453
        // Result = 0.453 / 0.001 = 453

        BigDecimal valueInBase = value.multiply(fromUom.getConversionFactor());
        return valueInBase.divide(toUom.getConversionFactor(), 6, RoundingMode.HALF_UP);
    }

    private void validateNoExistingBase(UnitOfMeasure.UomCategory category, UUID excludeId) {
        uomRepository.findByCategoryAndIsBaseTrue(category)
                .ifPresent(existingBase -> {
                    if (excludeId == null || !existingBase.getId().equals(excludeId)) {
                        throw new IllegalArgumentException("A base unit already exists for category: " + category);
                    }
                });
    }

    private UnitOfMeasureDto mapToDto(UnitOfMeasure uom) {
        UnitOfMeasureDto dto = new UnitOfMeasureDto();
        dto.setId(uom.getId());
        dto.setName(uom.getName());
        dto.setCode(uom.getCode());
        dto.setCategory(uom.getCategory());
        dto.setIsBase(uom.getIsBase());
        dto.setConversionFactor(uom.getConversionFactor());
        return dto;
    }
}
