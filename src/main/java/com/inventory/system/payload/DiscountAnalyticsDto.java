package com.inventory.system.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DiscountAnalyticsDto(
        LocalDateTime from,
        LocalDateTime to,
        long totalApplied,
        long totalFlagged,
        BigDecimal totalDiscountAmount,
        List<DiscountSummary> perDiscount
) {
    public record DiscountSummary(
            UUID discountId,
            String discountName,
            long appliedCount,
            BigDecimal totalDiscount,
            long uniqueCustomers
    ) {}
}
