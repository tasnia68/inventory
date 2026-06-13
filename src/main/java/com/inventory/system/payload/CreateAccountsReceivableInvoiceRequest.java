package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountsReceivableInvoiceRequest {
    private String sourceSystem;
    private String sourceDocumentType;
    private String sourcePartyId;
    private String sourcePartyName;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private UUID customerId;
    private UUID salesOrderId;
    private String customerInvoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal totalAmount;
    private UUID taxRateId;
    private String notes;
}
