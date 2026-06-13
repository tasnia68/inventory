package com.inventory.system.repository;

import com.inventory.system.common.entity.JournalEntry;
import com.inventory.system.common.entity.JournalEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID>, JpaSpecificationExecutor<JournalEntry> {
    Optional<JournalEntry> findByFinancialEventId(UUID financialEventId);
    long countByStatus(JournalEntryStatus status);
    boolean existsByReversalOfEntryId(UUID reversalOfEntryId);

    @Query("""
            select distinct e
            from JournalEntry e
            left join fetch e.lines l
            left join fetch l.account
            where e.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and (:from is null or e.entryDate >= :from)
              and (:to is null or e.entryDate <= :to)
            order by e.entryDate asc, e.entryNumber asc
            """)
    List<JournalEntry> findPostedEntriesWithLines(LocalDateTime from, LocalDateTime to);
}
