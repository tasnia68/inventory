package com.inventory.system.service.order.events;

import com.inventory.system.common.entity.SalesOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID salesOrderId,
        SalesOrderStatus fromStatus,
        SalesOrderStatus toStatus,
        LocalDateTime occurredAt
) {
}
