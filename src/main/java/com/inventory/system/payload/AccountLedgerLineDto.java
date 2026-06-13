package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AccountLedgerLineDto {
    private UUID lineId;
    private UUID journalEntryId;
    private String entryNumber;
    private LocalDateTime entryDate;
    private String journalCode;
    private String sourceDocumentType;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private String memo;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;
}
