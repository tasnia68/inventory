package com.inventory.system.payload;

import com.inventory.system.common.entity.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AccountsPayableInvoiceDto {
    private UUID id;
    private String invoiceNumber;
    private String supplierInvoiceNumber;
    private UUID supplierId;
    private String supplierName;
    private UUID purchaseOrderId;
    private String purchaseOrderNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private InvoiceStatus status;
    private String notes;
    private List<AccountsPayablePaymentDto> payments = new ArrayList<>();
}
