package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts_receivable_invoices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"invoice_number", "tenant_id"})
})
@Getter
@Setter
public class AccountsReceivableInvoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "customer_invoice_number")
    private String customerInvoiceNumber;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem = "INVENTORY";

    @Column(name = "source_document_type", nullable = false, length = 64)
    private String sourceDocumentType;

    @Column(name = "source_party_id", length = 128)
    private String sourcePartyId;

    @Column(name = "source_party_name")
    private String sourcePartyName;

    @Column(name = "source_document_id", length = 128)
    private String sourceDocumentId;

    @Column(name = "source_document_number")
    private String sourceDocumentNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "sales_order_number")
    private String salesOrderNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRate;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false, precision = 19, scale = 6)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountsReceivablePayment> payments = new ArrayList<>();
}
