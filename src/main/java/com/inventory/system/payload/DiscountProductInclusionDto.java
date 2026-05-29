package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountInclusionMode;
import com.inventory.system.common.entity.DiscountProductInclusionScope;

import java.util.UUID;

public record DiscountProductInclusionDto(
        UUID id,
        DiscountProductInclusionScope scope,
        UUID entityId,
        DiscountInclusionMode mode
) {}
