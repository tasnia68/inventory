package com.inventory.system.repository;

import com.inventory.system.common.entity.ReportConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportConfigurationRepository extends JpaRepository<ReportConfiguration, UUID>, JpaSpecificationExecutor<ReportConfiguration> {
    boolean existsByCode(String code);

    Optional<ReportConfiguration> findByCode(String code);

    List<ReportConfiguration> findByActiveTrueAndScheduleCronIsNotNull();
}