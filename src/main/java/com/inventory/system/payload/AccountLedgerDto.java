package com.inventory.system.payload;

import com.inventory.system.common.entity.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AccountLedgerDto {
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private LocalDate from;
    private LocalDate to;
    private int page;
    private int size;
    private long totalLines;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<AccountLedgerLineDto> lines = new ArrayList<>();
}
