package com.inventory.system.service.courier;

import java.math.BigDecimal;

public record CourierFeeQuote(
        BigDecimal customerCharge,
        BigDecimal courierCost,
        BigDecimal codFeePercent,
        BigDecimal codFee,
        BigDecimal totalCustomerPayable
) {
}
