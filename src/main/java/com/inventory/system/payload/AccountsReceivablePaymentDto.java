package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AccountsReceivablePaymentDto {
    private UUID id;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentReference;
    private String notes;
}
