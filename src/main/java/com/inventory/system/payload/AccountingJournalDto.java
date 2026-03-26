package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AccountingJournalDto {
    private UUID id;
    private String journalCode;
    private String journalName;
    private String description;
    private boolean systemJournal;
    private boolean active;
}
