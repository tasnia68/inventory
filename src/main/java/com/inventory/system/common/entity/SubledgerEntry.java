package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subledger_entries")
@Getter
@Setter
public class SubledgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_event_id", nullable = false)
    private FinancialEvent financialEvent;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private SubledgerEntryType entryType;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "source_document_type", nullable = false)
    private String sourceDocumentType;

    @Column(name = "source_document_id", nullable = false)
    private String sourceDocumentId;

    @Column(name = "source_document_number")
    private String sourceDocumentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false)
    private PostingStatus postingStatus = PostingStatus.PENDING;
}
