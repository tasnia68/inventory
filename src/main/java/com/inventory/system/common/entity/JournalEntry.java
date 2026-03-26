package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entry_number", "tenant_id"})
})
@Getter
@Setter
public class JournalEntry extends BaseEntity {

    @Column(name = "entry_number", nullable = false)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private AccountingJournal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_event_id")
    private FinancialEvent financialEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_entry_id")
    private JournalEntry reversalOfEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;

    @Column(name = "source_document_type", nullable = false)
    private String sourceDocumentType;

    @Column(name = "source_document_id", nullable = false)
    private String sourceDocumentId;

    @Column(name = "source_document_number")
    private String sourceDocumentNumber;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();
}
