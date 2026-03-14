package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_configurations")
@Getter
@Setter
public class ReportConfiguration extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportCategory category = ReportCategory.CUSTOM;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_type")
    private DashboardWidgetType widgetType;

    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    @Column(name = "filter_preset_json", columnDefinition = "TEXT")
    private String filterPresetJson;

    @Column(name = "columns_json", columnDefinition = "TEXT")
    private String columnsJson;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "shared_with", columnDefinition = "TEXT")
    private String sharedWith;

    @Column(name = "export_formats")
    private String exportFormats;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}