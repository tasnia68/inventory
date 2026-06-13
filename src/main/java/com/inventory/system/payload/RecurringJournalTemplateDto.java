package com.inventory.system.payload;

import com.inventory.system.common.entity.RecurringJournalCadence;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RecurringJournalTemplateDto {
    private UUID id;
    private String templateCode;
    private String templateName;
    private UUID journalId;
    private String journalCode;
    private String journalName;
    private String memo;
    private String currency;
    private RecurringJournalCadence cadence;
    private LocalDate nextRunDate;
    private LocalDateTime lastRunAt;
    private boolean active;
    private List<LineDto> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class LineDto {
        private UUID id;
        private Integer lineNumber;
        private UUID accountId;
        private String accountCode;
        private String accountName;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
    }
}
