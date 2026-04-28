package com.inventory.system.payload;

import lombok.Data;

@Data
public class ShipmentQueueSummaryDto {
    private long readyToHandoffCount;
    private long inTransitCount;
    private long needsActionCount;
    private long deliveryReviewPendingCount;
    private long deliveryReviewDisputedCount;
}