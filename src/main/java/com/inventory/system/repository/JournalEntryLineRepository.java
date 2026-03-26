package com.inventory.system.repository;

import com.inventory.system.common.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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
}
