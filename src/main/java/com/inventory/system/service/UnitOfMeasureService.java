package com.inventory.system.service;

import com.inventory.system.payload.UnitOfMeasureDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UnitOfMeasureService {
    UnitOfMeasureDto createUom(UnitOfMeasureDto dto);
    UnitOfMeasureDto updateUom(UUID id, UnitOfMeasureDto dto);
    void deleteUom(UUID id);
    UnitOfMeasureDto getUom(UUID id);
    List<UnitOfMeasureDto> getAllUoms();
    BigDecimal convert(BigDecimal value, UUID fromUomId, UUID toUomId);
}
