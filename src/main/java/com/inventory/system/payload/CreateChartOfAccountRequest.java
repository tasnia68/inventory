package com.inventory.system.payload;

import com.inventory.system.common.entity.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateChartOfAccountRequest {
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private UUID parentAccountId;
    private Boolean allowManualPosting;
    private Boolean active;
    private String description;
}
