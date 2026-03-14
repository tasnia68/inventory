package com.inventory.system.payload;

import com.inventory.system.common.entity.DashboardWidgetType;
import com.inventory.system.common.entity.ReportCategory;
import com.inventory.system.common.entity.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportConfigurationDto {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private ReportCategory category;
    private ReportType reportType;
    private DashboardWidgetType widgetType;
    private String configurationJson;
    private String filterPresetJson;
    private String columnsJson;
    private String scheduleCron;
    private String sharedWith;
    private String exportFormats;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}