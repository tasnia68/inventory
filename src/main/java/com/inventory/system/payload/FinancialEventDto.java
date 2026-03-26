package com.inventory.system.payload;

import com.inventory.system.common.entity.FinancialEventType;
import com.inventory.system.common.entity.PostingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FinancialEventDto {
    private UUID id;
    private String eventNumber;
    private FinancialEventType eventType;
    private String sourceDocumentType;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private String externalReference;
    private String summary;
    private BigDecimal totalAmount;
    private String currency;
    private PostingStatus postingStatus;
    private String failureReason;
    private LocalDateTime occurredAt;
    private String actorName;
    private String metadataJson;
    private LocalDateTime createdAt;
    private List<SubledgerEntryDto> subledgerEntries = new ArrayList<>();
}
