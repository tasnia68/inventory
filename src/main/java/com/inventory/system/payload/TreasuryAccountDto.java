package com.inventory.system.payload;

import com.inventory.system.common.entity.TreasuryAccountType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TreasuryAccountDto {
    private UUID id;
    private String accountCode;
    private String accountName;
    private TreasuryAccountType accountType;
    private String currency;
    private boolean active;
    private String notes;
}
