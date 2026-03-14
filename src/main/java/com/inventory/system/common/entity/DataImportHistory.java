package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_import_history")
@Getter
@Setter
public class DataImportHistory extends BaseEntity {

    @Column(nullable = false)
    private String dataset;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataImportStatus status = DataImportStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "processed_records")
    private Integer processedRecords;

    @Column(name = "successful_records")
    private Integer successfulRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "summary_message", columnDefinition = "TEXT")
    private String summaryMessage;
}