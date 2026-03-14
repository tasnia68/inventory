package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DamageRecordDocumentDto {
    private UUID id;
    private UUID damageRecordId;
    private String recordNumber;
    private String documentType;
    private String filename;
    private String contentType;
    private String storagePath;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}