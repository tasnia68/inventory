package com.inventory.system.accounting.api.event;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FinancialEventDto {
    private String eventType;
    private String sourceDocumentType;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private String externalReference;
    private String summary;
    private BigDecimal totalAmount;
    private String currency;
    private String metadataJson;
    private List<LineDto> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class LineDto {
        private String entryType;
        private String accountCode;
        private String accountName;
        private String description;
        private BigDecimal amount;
        private String currency;
    }
}