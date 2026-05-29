package com.inventory.system.payload;

import com.inventory.system.common.entity.DiscountCustomerInclusionScope;
import com.inventory.system.common.entity.DiscountInclusionMode;

import java.util.UUID;

public record DiscountCustomerInclusionDto(
        UUID id,
        DiscountCustomerInclusionScope scope,
        String entityId,
        DiscountInclusionMode mode
) {}
