package com.inventory.system.service.order.events;

import com.inventory.system.common.entity.SalesChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published immediately after a SalesOrder is persisted (any channel: storefront, manual, POS).
 * Side-effect listeners (referral attribution, loyalty, notifications) should react to this.
 */
public record SalesOrderCreatedEvent(
        UUID salesOrderId,
        UUID customerId,
        SalesChannel channel,
        String referralCode,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
}
