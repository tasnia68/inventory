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
public class AccountsReceivableInvoiceDto {
    private UUID id;
    private String invoiceNumber;
    private String customerInvoiceNumber;
    private UUID customerId;
    private String customerName;
    private UUID salesOrderId;
    private String salesOrderNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private InvoiceStatus status;
    private String notes;
    private List<AccountsReceivablePaymentDto> payments = new ArrayList<>();
}
