package com.inventory.system.payload;

import com.inventory.system.common.entity.DashboardWidgetType;
import com.inventory.system.common.entity.ReportType;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class DashboardWidgetDto {
    private UUID configurationId;
    private String title;
    private DashboardWidgetType widgetType;
    private ReportType reportType;
    private Integer displayOrder;
    private Map<String, Object> data;
}