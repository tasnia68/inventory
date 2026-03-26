package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateManualJournalEntryRequest {
    private UUID journalId;
    private LocalDateTime entryDate;
    private String memo;
    private String currency;
    private List<LineRequest> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class LineRequest {
        private UUID accountId;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
    }
}
