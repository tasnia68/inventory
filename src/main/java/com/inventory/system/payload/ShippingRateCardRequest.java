package com.inventory.system.payload;

import com.inventory.system.common.entity.DeliveryZone;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShippingRateCardRequest {

    @NotNull
    private DeliveryZone zone;

    @NotNull
    private BigDecimal customerCharge;

    @NotNull
    private BigDecimal courierCost;

    private BigDecimal codFeePercent = BigDecimal.ZERO;
    private BigDecimal weightKgIncluded;
    private BigDecimal perKgOverage;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
