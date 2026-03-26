package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_events")
@Getter
@Setter
public class FinancialEvent extends BaseEntity {

    @Column(name = "event_number", nullable = false, unique = true)
    private String eventNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private FinancialEventType eventType;

    @Column(name = "source_document_type", nullable = false)
    private String sourceDocumentType;

    @Column(name = "source_document_id", nullable = false)
    private String sourceDocumentId;

    @Column(name = "source_document_number")
    private String sourceDocumentNumber;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false)
    private PostingStatus postingStatus = PostingStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @OneToMany(mappedBy = "financialEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubledgerEntry> subledgerEntries = new ArrayList<>();
}
