package com.inventory.system.service;

import com.inventory.system.common.entity.ReportConfiguration;
import com.inventory.system.common.entity.ReportShare;
import com.inventory.system.common.entity.User;
import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ReportShareDto;
import com.inventory.system.payload.ShareReportRequest;
import com.inventory.system.repository.ReportConfigurationRepository;
import com.inventory.system.repository.ReportShareRepository;
import com.inventory.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportSharingServiceImpl implements ReportSharingService {

    private final ReportConfigurationRepository reportConfigurationRepository;
    private final ReportShareRepository reportShareRepository;
    private final UserRepository userRepository;
    private final WebhookService webhookService;

    @Override
    @Transactional(readOnly = true)
    public List<ReportShareDto> getShares(UUID reportConfigurationId) {
        return reportShareRepository.findByReportConfigurationId(reportConfigurationId).stream()
                .map(this::mapShare)
                .toList();
    }

    @Override
    @Transactional
    public ReportShareDto shareReport(UUID reportConfigurationId, ShareReportRequest request) {
        if (request == null || request.getSharedWithUserId() == null) {
            throw new BadRequestException("Shared user is required");
        }

        ReportConfiguration configuration = reportConfigurationRepository.findById(reportConfigurationId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportConfiguration", "id", reportConfigurationId));
        User user = userRepository.findById(request.getSharedWithUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getSharedWithUserId()));

        reportShareRepository.findByReportConfigurationIdAndSharedWithUserId(reportConfigurationId, request.getSharedWithUserId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Report is already shared with this user");
                });

        ReportShare share = new ReportShare();
        share.setReportConfiguration(configuration);
        share.setSharedWithUser(user);
        share.setAccessLevel(request.getAccessLevel().trim().toUpperCase());
        ReportShare saved = reportShareRepository.save(share);

        webhookService.publishEvent(WebhookEventType.REPORT_SHARED, Map.of(
                "reportConfigurationId", configuration.getId(),
                "reportCode", configuration.getCode(),
                "sharedWithUserId", user.getId(),
                "sharedWithUserEmail", user.getEmail(),
                "accessLevel", saved.getAccessLevel()));

        return mapShare(saved);
    }

    @Override
    @Transactional
    public void revokeShare(UUID shareId) {
        ReportShare share = reportShareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportShare", "id", shareId));
        reportShareRepository.delete(share);
    }

    private ReportShareDto mapShare(ReportShare share) {
        ReportShareDto dto = new ReportShareDto();
        dto.setId(share.getId());
        dto.setReportConfigurationId(share.getReportConfiguration().getId());
        dto.setSharedWithUserId(share.getSharedWithUser().getId());
        dto.setSharedWithUserEmail(share.getSharedWithUser().getEmail());
        dto.setAccessLevel(share.getAccessLevel());
        dto.setCreatedAt(share.getCreatedAt());
        return dto;
    }
}