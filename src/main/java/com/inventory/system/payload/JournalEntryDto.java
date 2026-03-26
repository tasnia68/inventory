package com.inventory.system.payload;

import com.inventory.system.common.entity.JournalEntryStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class JournalEntryDto {
    private UUID id;
    private String entryNumber;
    private UUID journalId;
    private String journalCode;
    private String journalName;
    private UUID financialEventId;
    private UUID reversalOfEntryId;
    private JournalEntryStatus status;
    private LocalDateTime entryDate;
    private String sourceDocumentType;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private String memo;
    private String currency;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private LocalDateTime postedAt;
    private List<JournalEntryLineDto> lines = new ArrayList<>();
}
