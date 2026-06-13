package com.inventory.system.repository;

import com.inventory.system.common.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, UUID> {
    List<JournalEntryLine> findByJournalEntryIdOrderByLineNumberAsc(UUID journalEntryId);

    @Query("""
            select coalesce(sum(l.debitAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
            """)
    BigDecimal sumDebitsByAccount(UUID accountId);

    @Query("""
            select coalesce(sum(l.creditAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
            """)
    BigDecimal sumCreditsByAccount(UUID accountId);

    @Query("""
            select l
            from JournalEntryLine l
            join fetch l.journalEntry e
            join fetch e.journal j
            join fetch l.account a
            where a.id = :accountId
              and e.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and (:from is null or e.entryDate >= :from)
              and (:to is null or e.entryDate <= :to)
            order by e.entryDate asc, e.entryNumber asc, l.lineNumber asc
            """)
    List<JournalEntryLine> findPostedLedgerLines(UUID accountId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select coalesce(sum(l.debitAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId
              and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and l.journalEntry.entryDate < :before
            """)
    BigDecimal sumDebitsByAccountBefore(UUID accountId, LocalDateTime before);

    @Query("""
            select coalesce(sum(l.creditAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId
              and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and l.journalEntry.entryDate < :before
            """)
    BigDecimal sumCreditsByAccountBefore(UUID accountId, LocalDateTime before);

    @Query("""
            select coalesce(sum(l.debitAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId
              and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and (:from is null or l.journalEntry.entryDate >= :from)
              and (:to is null or l.journalEntry.entryDate <= :to)
            """)
    BigDecimal sumDebitsByAccountBetween(UUID accountId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select coalesce(sum(l.creditAmount), 0)
            from JournalEntryLine l
            where l.account.id = :accountId
              and l.journalEntry.status = com.inventory.system.common.entity.JournalEntryStatus.POSTED
              and (:from is null or l.journalEntry.entryDate >= :from)
              and (:to is null or l.journalEntry.entryDate <= :to)
            """)
    BigDecimal sumCreditsByAccountBetween(UUID accountId, LocalDateTime from, LocalDateTime to);
}
