package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecordAccountsPayablePaymentRequest {
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentReference;
    private String notes;
}
