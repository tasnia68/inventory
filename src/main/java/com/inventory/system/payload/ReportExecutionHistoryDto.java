package com.inventory.system.payload;

import com.inventory.system.common.entity.ReportExecutionStatus;
import com.inventory.system.common.entity.ReportOutputFormat;
import com.inventory.system.common.entity.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportExecutionHistoryDto {
    private UUID id;
    private UUID reportConfigurationId;
    private String reportName;
    private ReportType reportType;
    private ReportOutputFormat outputFormat;
    private ReportExecutionStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private Integer rowCount;
    private String filtersJson;
    private String errorMessage;
    private String createdBy;
}