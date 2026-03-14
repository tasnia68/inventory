package com.inventory.system.repository;

import com.inventory.system.common.entity.ReportShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportShareRepository extends JpaRepository<ReportShare, UUID> {
    List<ReportShare> findByReportConfigurationId(UUID reportConfigurationId);

    List<ReportShare> findBySharedWithUserId(UUID userId);

    Optional<ReportShare> findByReportConfigurationIdAndSharedWithUserId(UUID reportConfigurationId, UUID sharedWithUserId);
}