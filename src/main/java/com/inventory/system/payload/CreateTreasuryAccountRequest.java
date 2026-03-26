package com.inventory.system.payload;

import com.inventory.system.common.entity.TreasuryAccountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTreasuryAccountRequest {
    private String accountCode;
    private String accountName;
    private TreasuryAccountType accountType;
    private String currency;
    private Boolean active;
    private String notes;
}
