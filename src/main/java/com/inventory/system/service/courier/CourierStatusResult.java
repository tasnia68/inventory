package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.DeliveryReviewStatus;

import java.time.LocalDateTime;

public record CourierStatusResult(
        CourierDispatchStatus dispatchStatus,
        DeliveryReviewStatus reviewStatus,
        String reviewReason,
        String lastCourierEvent,
        LocalDateTime syncedAt
) {
}
