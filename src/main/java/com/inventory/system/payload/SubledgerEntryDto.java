package com.inventory.system.payload;

import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.SubledgerEntryType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SubledgerEntryDto {
    private UUID id;
    private Integer lineNumber;
    private SubledgerEntryType entryType;
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String sourceDocumentType;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private PostingStatus postingStatus;
}
