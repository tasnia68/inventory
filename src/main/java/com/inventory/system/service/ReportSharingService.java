package com.inventory.system.service;

import com.inventory.system.payload.ReportShareDto;
import com.inventory.system.payload.ShareReportRequest;

import java.util.List;
import java.util.UUID;

public interface ReportSharingService {
    List<ReportShareDto> getShares(UUID reportConfigurationId);

    ReportShareDto shareReport(UUID reportConfigurationId, ShareReportRequest request);

    void revokeShare(UUID shareId);
}