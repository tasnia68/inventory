package com.inventory.system.payload;

import com.inventory.system.common.entity.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GeneratedReportDto {
    private String reportName;
    private ReportType reportType;
    private LocalDateTime generatedAt;
    private List<String> headers;
    private List<Map<String, Object>> rows;
    private Map<String, Object> summary;
}