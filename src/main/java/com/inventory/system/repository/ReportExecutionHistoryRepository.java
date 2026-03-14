package com.inventory.system.repository;

import com.inventory.system.common.entity.ReportExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportExecutionHistoryRepository extends JpaRepository<ReportExecutionHistory, UUID> {
    List<ReportExecutionHistory> findTop100ByOrderByCreatedAtDesc();

    Optional<ReportExecutionHistory> findTopByReportConfigurationIdOrderByRequestedAtDesc(UUID reportConfigurationId);
}