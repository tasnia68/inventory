package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AccountingAuditLogDto {
    private UUID id;
    private String entityType;
    private String entityId;
    private String action;
    private String beforeState;
    private String afterState;
    private String userId;
    private LocalDateTime occurredAt;
}
