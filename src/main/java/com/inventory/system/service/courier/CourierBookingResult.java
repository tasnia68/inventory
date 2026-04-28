package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierDispatchStatus;

import java.time.LocalDateTime;

public record CourierBookingResult(
        String providerCode,
        String courierReference,
        String trackingNumber,
        String trackingUrl,
        CourierDispatchStatus dispatchStatus,
        String lastCourierEvent,
        LocalDateTime bookedAt
) {
}
