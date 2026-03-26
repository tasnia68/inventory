package com.inventory.system.payload;

import com.inventory.system.common.entity.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class FinancialStatementRowDto {
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private BigDecimal amount;
}
