package com.inventory.system.repository;

import com.inventory.system.common.entity.JournalEntry;
import com.inventory.system.common.entity.JournalEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID>, JpaSpecificationExecutor<JournalEntry> {
    Optional<JournalEntry> findByFinancialEventId(UUID financialEventId);
    long countByStatus(JournalEntryStatus status);
    boolean existsByReversalOfEntryId(UUID reversalOfEntryId);
}
