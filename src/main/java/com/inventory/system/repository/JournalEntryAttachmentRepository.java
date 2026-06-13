package com.inventory.system.repository;

import com.inventory.system.common.entity.JournalEntryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalEntryAttachmentRepository extends JpaRepository<JournalEntryAttachment, UUID> {
    List<JournalEntryAttachment> findByJournalEntryIdOrderByCreatedAtDesc(UUID journalEntryId);
}
