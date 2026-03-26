package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountingJournalRequest {
    private String journalCode;
    private String journalName;
    private String description;
    private Boolean active;
}
