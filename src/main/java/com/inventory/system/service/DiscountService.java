package com.inventory.system.service;

import com.inventory.system.common.entity.DiscountChannel;
import com.inventory.system.payload.CreateDiscountCodeRequest;
import com.inventory.system.payload.CreateDiscountRequest;
import com.inventory.system.payload.DiscountAnalyticsDto;
import com.inventory.system.payload.DiscountCodeDto;
import com.inventory.system.payload.DiscountDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DiscountService {

    DiscountDto create(CreateDiscountRequest request);

    DiscountDto update(UUID id, CreateDiscountRequest request);

    void delete(UUID id);

    DiscountDto get(UUID id);

    List<DiscountDto> list();

    List<DiscountDto> listAvailable(DiscountChannel channel);

    DiscountCodeDto createCode(UUID discountId, CreateDiscountCodeRequest request);

    DiscountCodeDto updateCode(UUID codeId, CreateDiscountCodeRequest request);

    void deleteCode(UUID codeId);

    List<DiscountCodeDto> listCodes(UUID discountId);

    DiscountAnalyticsDto analytics(LocalDateTime from, LocalDateTime to);
}
