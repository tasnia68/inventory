package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class JournalEntryAttachmentDto {
    private UUID id;
    private UUID journalEntryId;
    private String entryNumber;
    private String filename;
    private String contentType;
    private String storagePath;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
