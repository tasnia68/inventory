package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountingJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountingJournalRepository extends JpaRepository<AccountingJournal, UUID> {
    Optional<AccountingJournal> findByJournalCode(String journalCode);
}
