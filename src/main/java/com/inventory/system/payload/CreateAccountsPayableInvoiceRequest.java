package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountsPayableInvoiceRequest {
    private String sourceSystem;
    private String sourceDocumentType;
    private String sourcePartyId;
    private String sourcePartyName;
    private String sourceDocumentId;
    private String sourceDocumentNumber;
    private UUID supplierId;
    private UUID purchaseOrderId;
    private String supplierInvoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal totalAmount;
    private UUID taxRateId;
    private String notes;
    /** When true, bypass three-way match validation. Override is logged into invoice notes. */
    private boolean force = false;
}
