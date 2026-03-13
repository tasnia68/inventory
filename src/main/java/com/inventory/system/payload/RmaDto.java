package com.inventory.system.payload;

import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RmaDto {
    private UUID id;
    private String rmaNumber;
    private UUID salesOrderId;
    private String soNumber;
    private UUID shipmentId;
    private String shipmentNumber;
    private ReturnMerchandiseStatus status;
    private String reason;
    private String notes;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private List<RmaItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}