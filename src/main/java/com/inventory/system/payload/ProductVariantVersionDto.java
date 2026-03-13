package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductVariantVersionDto {
    private UUID id;
    private UUID productVariantId;
    private Integer versionNumber;
    private String changeType;
    private String snapshot;
    private LocalDateTime createdAt;
    private String createdBy;
}