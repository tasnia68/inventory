package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportShareDto {
    private UUID id;
    private UUID reportConfigurationId;
    private UUID sharedWithUserId;
    private String sharedWithUserEmail;
    private String accessLevel;
    private LocalDateTime createdAt;
}