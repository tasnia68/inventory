package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_execution_history")
@Getter
@Setter
public class ReportExecutionHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_configuration_id")
    private ReportConfiguration reportConfiguration;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false)
    private ReportOutputFormat outputFormat = ReportOutputFormat.CSV;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportExecutionStatus status = ReportExecutionStatus.SUCCESS;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "filters_json", columnDefinition = "TEXT")
    private String filtersJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}