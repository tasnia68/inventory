package com.inventory.system.service.courier;

import java.time.LocalDateTime;

public record CourierReturnResult(
        String providerCode,
        String returnRequestId,
        String consignmentReference,
        String status,
        String reason,
        LocalDateTime requestedAt
) {
}
