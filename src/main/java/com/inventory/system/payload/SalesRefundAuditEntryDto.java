package com.inventory.system.payload;

import com.inventory.system.common.entity.SalesRefundAuditAction;
import com.inventory.system.common.entity.SalesRefundStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SalesRefundAuditEntryDto {
    private UUID id;
    private SalesRefundAuditAction action;
    private SalesRefundStatus fromStatus;
    private SalesRefundStatus toStatus;
    private String notes;
    private LocalDateTime actedAt;
    private String createdBy;
}