package com.inventory.system.payload;

import com.inventory.system.common.entity.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChartOfAccountDto {
    private UUID id;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private UUID parentAccountId;
    private String parentAccountCode;
    private boolean allowManualPosting;
    private boolean active;
    private String description;
}
