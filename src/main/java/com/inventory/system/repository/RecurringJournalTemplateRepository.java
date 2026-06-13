package com.inventory.system.repository;

import com.inventory.system.common.entity.RecurringJournalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringJournalTemplateRepository extends JpaRepository<RecurringJournalTemplate, UUID>, JpaSpecificationExecutor<RecurringJournalTemplate> {
    Optional<RecurringJournalTemplate> findByTemplateCode(String templateCode);
    List<RecurringJournalTemplate> findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(LocalDate runDate);
    List<RecurringJournalTemplate> findByTenantIdAndActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(String tenantId, LocalDate runDate);
}
