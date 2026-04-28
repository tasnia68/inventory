package com.inventory.system.payload;

import com.inventory.system.common.entity.ShipmentQueueType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ShipmentQueueRefreshResultDto {
    private ShipmentQueueType queue;
    private int requestedCount;
    private int refreshedCount;
    private int bookedCount;
    private int syncedCount;
    private int skippedCount;
    private int failedCount;
    private LocalDateTime refreshedAt;
    private List<String> failures = new ArrayList<>();
}