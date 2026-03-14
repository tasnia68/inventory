package com.inventory.system.payload;

import com.inventory.system.common.entity.DataImportStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DataImportHistoryDto {
    private UUID id;
    private String dataset;
    private String fileName;
    private DataImportStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer successfulRecords;
    private Integer failedRecords;
    private String validationErrors;
    private String summaryMessage;
    private String createdBy;
}