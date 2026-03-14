package com.inventory.system.service;

import com.inventory.system.common.entity.ReportConfiguration;
import com.inventory.system.common.entity.ReportExecutionHistory;
import com.inventory.system.common.entity.ReportOutputFormat;
import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.payload.GenerateReportRequest;
import com.inventory.system.repository.ReportConfigurationRepository;
import com.inventory.system.repository.ReportExecutionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportSchedulingServiceImpl implements ReportSchedulingService {

    private final ReportConfigurationRepository reportConfigurationRepository;
    private final ReportExecutionHistoryRepository reportExecutionHistoryRepository;
    private final ReportingService reportingService;
    private final WebhookService webhookService;

    @Override
    @Scheduled(fixedDelay = 60000)
    public void processScheduledReports() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        for (ReportConfiguration configuration : reportConfigurationRepository.findByActiveTrueAndScheduleCronIsNotNull()) {
            if (configuration.getScheduleCron() == null || configuration.getScheduleCron().isBlank()) {
                continue;
            }

            CronExpression cronExpression;
            try {
                cronExpression = CronExpression.parse(configuration.getScheduleCron());
            } catch (IllegalArgumentException exception) {
                continue;
            }

            LocalDateTime expectedRunTime = cronExpression.next(now.minusMinutes(1));
            if (expectedRunTime == null || expectedRunTime.isAfter(now)) {
                continue;
            }

            ReportExecutionHistory lastExecution = reportExecutionHistoryRepository
                    .findTopByReportConfigurationIdOrderByRequestedAtDesc(configuration.getId())
                    .orElse(null);
            if (lastExecution != null && !lastExecution.getRequestedAt().isBefore(now.minusMinutes(1))) {
                continue;
            }

            GenerateReportRequest request = new GenerateReportRequest();
            request.setConfigurationId(configuration.getId());
            request.setFormat(resolveFormat(configuration));
            reportingService.exportReportFile(request);
            webhookService.publishEvent(WebhookEventType.REPORT_SCHEDULED, Map.of(
                    "reportConfigurationId", configuration.getId(),
                    "reportCode", configuration.getCode(),
                    "reportType", configuration.getReportType().name(),
                    "scheduledAt", now.toString()));
        }
    }

    private ReportOutputFormat resolveFormat(ReportConfiguration configuration) {
        if (configuration.getExportFormats() == null || configuration.getExportFormats().isBlank()) {
            return ReportOutputFormat.CSV;
        }
        try {
            return Arrays.stream(configuration.getExportFormats().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .map(String::toUpperCase)
                .map(ReportOutputFormat::valueOf)
                .orElse(ReportOutputFormat.CSV);
        } catch (IllegalArgumentException exception) {
            return ReportOutputFormat.CSV;
        }
    }
}