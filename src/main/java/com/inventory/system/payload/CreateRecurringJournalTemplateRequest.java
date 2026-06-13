package com.inventory.system.payload;

import com.inventory.system.common.entity.RecurringJournalCadence;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateRecurringJournalTemplateRequest {
    private String templateCode;
    private String templateName;
    private UUID journalId;
    private String memo;
    private String currency;
    private RecurringJournalCadence cadence;
    private LocalDate nextRunDate;
    private Boolean active;
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
