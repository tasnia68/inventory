package com.inventory.system.payload;

import com.inventory.system.common.entity.DeliveryZone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateCardDto {
    private UUID id;
    private UUID courierProfileId;
    private DeliveryZone zone;
    private BigDecimal customerCharge;
    private BigDecimal courierCost;
    private BigDecimal codFeePercent;
    private BigDecimal weightKgIncluded;
    private BigDecimal perKgOverage;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
