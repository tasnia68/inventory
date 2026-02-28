package com.inventory.system.payload;

import com.inventory.system.common.entity.RmaStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ReturnAuthorizationDto {
    private UUID id;
    private String rmaNumber;
    private UUID salesOrderId;
    private String soNumber;
    private UUID customerId;
    private String customerName;
    private RmaStatus status;
    private String reason;
    private LocalDateTime requestDate;
    private LocalDateTime approvedDate;
    private LocalDateTime receivedDate;
    private String notes;
    private List<ReturnItemDto> items;
}
