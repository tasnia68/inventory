package com.inventory.system.service;

import com.inventory.system.common.entity.AccountType;
import com.inventory.system.common.entity.AccountingJournal;
import com.inventory.system.common.entity.AccountsPayableInvoice;
import com.inventory.system.common.entity.AccountsPayablePayment;
import com.inventory.system.common.entity.AccountsReceivableInvoice;
import com.inventory.system.common.entity.AccountsReceivablePayment;
import com.inventory.system.common.entity.ChartOfAccount;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.FinancialEvent;
import com.inventory.system.common.entity.InvoiceStatus;
import com.inventory.system.common.entity.JournalEntry;
import com.inventory.system.common.entity.JournalEntryLine;
import com.inventory.system.common.entity.JournalEntryStatus;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.PosPaymentMethod;
import com.inventory.system.common.entity.PosShift;
import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.ReconciliationSourceType;
import com.inventory.system.common.entity.ReconciliationStatus;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SubledgerEntry;
import com.inventory.system.common.entity.SubledgerEntryType;
import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.entity.TreasuryAccount;
import com.inventory.system.common.entity.TreasuryAccountType;
import com.inventory.system.common.entity.TreasuryReconciliation;
import com.inventory.system.common.entity.TreasuryReconciliationLine;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AccountingJournalDto;
import com.inventory.system.payload.AccountsPayableInvoiceDto;
import com.inventory.system.payload.AccountsPayablePaymentDto;
import com.inventory.system.payload.AccountsReceivableInvoiceDto;
import com.inventory.system.payload.AccountsReceivablePaymentDto;
import com.inventory.system.payload.AgingSummaryRowDto;
import com.inventory.system.payload.ChartOfAccountDto;
import com.inventory.system.payload.CompleteTreasuryReconciliationRequest;
import com.inventory.system.payload.CreateAccountsPayableInvoiceRequest;
import com.inventory.system.payload.CreateAccountsReceivableInvoiceRequest;
import com.inventory.system.payload.CreateAccountingJournalRequest;
import com.inventory.system.payload.CreateChartOfAccountRequest;
import com.inventory.system.payload.CreateManualJournalEntryRequest;
import com.inventory.system.payload.CreateTreasuryAccountRequest;
import com.inventory.system.payload.CreateTreasuryReconciliationRequest;
import com.inventory.system.payload.FinancialStatementRowDto;
import com.inventory.system.payload.JournalEntryDto;
import com.inventory.system.payload.JournalEntryLineDto;
import com.inventory.system.payload.RecordAccountsPayablePaymentRequest;
import com.inventory.system.payload.RecordAccountsReceivablePaymentRequest;
import com.inventory.system.payload.TreasuryAccountDto;
import com.inventory.system.payload.TreasuryReconciliationDto;
import com.inventory.system.payload.TreasuryReconciliationLineDto;
import com.inventory.system.payload.TrialBalanceRowDto;
import com.inventory.system.repository.AccountsPayableInvoiceRepository;
import com.inventory.system.repository.AccountsPayablePaymentRepository;
import com.inventory.system.repository.AccountsReceivableInvoiceRepository;
import com.inventory.system.repository.AccountsReceivablePaymentRepository;
import com.inventory.system.repository.AccountingJournalRepository;
import com.inventory.system.repository.ChartOfAccountRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.FinancialEventRepository;
import com.inventory.system.repository.JournalEntryLineRepository;
import com.inventory.system.repository.JournalEntryRepository;
import com.inventory.system.repository.PosShiftRepository;
import com.inventory.system.repository.PurchaseOrderRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.SupplierRepository;
import com.inventory.system.repository.TreasuryAccountRepository;
import com.inventory.system.repository.TreasuryReconciliationLineRepository;
import com.inventory.system.repository.TreasuryReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountingServiceImpl implements AccountingService {

    private static final String SYSTEM_JOURNAL_CODE = "GL-SYSTEM";
    private static final String ACCOUNTS_PAYABLE_JOURNAL_CODE = "AP";
    private static final String ACCOUNTS_RECEIVABLE_JOURNAL_CODE = "AR";

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountingJournalRepository accountingJournalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final FinancialEventRepository financialEventRepository;
    private final AccountsPayableInvoiceRepository accountsPayableInvoiceRepository;
    private final AccountsPayablePaymentRepository accountsPayablePaymentRepository;
    private final AccountsReceivableInvoiceRepository accountsReceivableInvoiceRepository;
    private final AccountsReceivablePaymentRepository accountsReceivablePaymentRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PosShiftRepository posShiftRepository;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final TreasuryReconciliationRepository treasuryReconciliationRepository;
    private final TreasuryReconciliationLineRepository treasuryReconciliationLineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountDto> getAccounts() {
        return chartOfAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(ChartOfAccount::getAccountCode))
                .map(this::mapAccount)
                .toList();
    }

    @Override
    @Transactional
    public ChartOfAccountDto createAccount(CreateChartOfAccountRequest request) {
        if (!StringUtils.hasText(request.getAccountCode()) || !StringUtils.hasText(request.getAccountName()) || request.getAccountType() == null) {
            throw new BadRequestException("Account code, name, and type are required");
        }
        if (chartOfAccountRepository.findByAccountCode(request.getAccountCode().trim().toUpperCase(Locale.ROOT)).isPresent()) {
            throw new BadRequestException("Account code already exists");
        }

        ChartOfAccount account = new ChartOfAccount();
        account.setAccountCode(request.getAccountCode().trim().toUpperCase(Locale.ROOT));
        account.setAccountName(request.getAccountName().trim());
        account.setAccountType(request.getAccountType());
        account.setAllowManualPosting(request.getAllowManualPosting() == null || request.getAllowManualPosting());
        account.setActive(request.getActive() == null || request.getActive());
        account.setDescription(blankToNull(request.getDescription()));
        if (request.getParentAccountId() != null) {
            account.setParentAccount(chartOfAccountRepository.findById(request.getParentAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", request.getParentAccountId())));
        }
        return mapAccount(chartOfAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingJournalDto> getJournals() {
        return accountingJournalRepository.findAll().stream()
                .sorted(Comparator.comparing(AccountingJournal::getJournalCode))
                .map(this::mapJournal)
                .toList();
    }

    @Override
    @Transactional
    public AccountingJournalDto createJournal(CreateAccountingJournalRequest request) {
        if (!StringUtils.hasText(request.getJournalCode()) || !StringUtils.hasText(request.getJournalName())) {
            throw new BadRequestException("Journal code and name are required");
        }
        if (accountingJournalRepository.findByJournalCode(request.getJournalCode().trim().toUpperCase(Locale.ROOT)).isPresent()) {
            throw new BadRequestException("Journal code already exists");
        }

        AccountingJournal journal = new AccountingJournal();
        journal.setJournalCode(request.getJournalCode().trim().toUpperCase(Locale.ROOT));
        journal.setJournalName(request.getJournalName().trim());
        journal.setDescription(blankToNull(request.getDescription()));
        journal.setActive(request.getActive() == null || request.getActive());
        return mapJournal(accountingJournalRepository.save(journal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryDto> getJournalEntries() {
        return journalEntryRepository.findAll().stream()
                .sorted(Comparator.comparing(JournalEntry::getEntryDate).reversed())
                .map(this::mapEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountsPayableInvoiceDto> getAccountsPayableInvoices() {
        return accountsPayableInvoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(AccountsPayableInvoice::getInvoiceDate).reversed()
                        .thenComparing(AccountsPayableInvoice::getInvoiceNumber).reversed())
                .map(this::mapAccountsPayableInvoice)
                .toList();
    }

    @Override
    @Transactional
    public AccountsPayableInvoiceDto createAccountsPayableInvoice(CreateAccountsPayableInvoiceRequest request) {
        if (request.getSupplierId() == null && request.getPurchaseOrderId() == null) {
            throw new BadRequestException("Supplier or purchase order is required");
        }

        PurchaseOrder purchaseOrder = request.getPurchaseOrderId() == null ? null : purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", request.getPurchaseOrderId()));
        Supplier supplier = request.getSupplierId() != null
                ? supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()))
                : purchaseOrder.getSupplier();
        if (purchaseOrder != null && !purchaseOrder.getSupplier().getId().equals(supplier.getId())) {
            throw new BadRequestException("Purchase order supplier does not match the selected supplier");
        }

        BigDecimal totalAmount = scale(request.getTotalAmount() != null ? request.getTotalAmount() : purchaseOrder == null ? null : purchaseOrder.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Accounts payable invoice amount must be greater than zero");
        }

        LocalDate invoiceDate = request.getInvoiceDate() == null ? LocalDate.now() : request.getInvoiceDate();
        AccountsPayableInvoice invoice = new AccountsPayableInvoice();
        invoice.setInvoiceNumber(generateDocumentNumber("APV"));
        invoice.setSupplierInvoiceNumber(blankToNull(request.getSupplierInvoiceNumber()));
        invoice.setSupplier(supplier);
        invoice.setPurchaseOrder(purchaseOrder);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.getDueDate() == null ? invoiceDate.plusDays(30) : request.getDueDate());
        invoice.setCurrency(defaultCurrency(request.getCurrency() != null ? request.getCurrency() : purchaseOrder == null ? null : purchaseOrder.getCurrency()));
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        invoice.setBalanceDue(totalAmount);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setNotes(blankToNull(request.getNotes()));
        invoice = accountsPayableInvoiceRepository.save(invoice);

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_PAYABLE_JOURNAL_CODE, "Accounts Payable Journal", "System journal for AP invoice and payment posting"),
                "AP_INVOICE",
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts payable invoice " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                List.of(
                        new JournalLineSpec("LIABILITY-GRN-ACCRUAL", "Goods receipt accrual clearing", totalAmount, BigDecimal.ZERO),
                        new JournalLineSpec("LIABILITY-AP-TRADE", "Trade accounts payable", BigDecimal.ZERO, totalAmount)
                )
        );

        return mapAccountsPayableInvoice(invoice);
    }

    @Override
    @Transactional
    public AccountsPayableInvoiceDto recordAccountsPayablePayment(UUID invoiceId, RecordAccountsPayablePaymentRequest request) {
        AccountsPayableInvoice invoice = accountsPayableInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountsPayableInvoice", "id", invoiceId));
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Payment cannot be recorded for a closed accounts payable invoice");
        }

        BigDecimal amount = scale(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new BadRequestException("Payment amount cannot exceed the invoice balance due");
        }

        AccountsPayablePayment payment = new AccountsPayablePayment();
        payment.setInvoice(invoice);
        payment.setPaymentDate(request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate());
        payment.setAmount(amount);
        payment.setPaymentMethod(blankToNull(request.getPaymentMethod()));
        payment.setPaymentReference(blankToNull(request.getPaymentReference()));
        payment.setNotes(blankToNull(request.getNotes()));
        payment = accountsPayablePaymentRepository.save(payment);
        invoice.getPayments().add(payment);
        applyPaymentToInvoice(invoice, amount);
        invoice = accountsPayableInvoiceRepository.save(invoice);

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_PAYABLE_JOURNAL_CODE, "Accounts Payable Journal", "System journal for AP invoice and payment posting"),
                "AP_PAYMENT",
                payment.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts payable payment for " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                List.of(
                        new JournalLineSpec("LIABILITY-AP-TRADE", "Trade accounts payable settlement", amount, BigDecimal.ZERO),
                        new JournalLineSpec("ASSET-CASH", "Cash disbursement", BigDecimal.ZERO, amount)
                )
        );

        return mapAccountsPayableInvoice(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingSummaryRowDto> getAccountsPayableAging() {
        Map<UUID, AgingSummaryRowDto> rows = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        accountsPayableInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(invoice -> invoice.getSupplier().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(invoice -> applyAging(
                        rows.computeIfAbsent(invoice.getSupplier().getId(), ignored -> createAgingRow(invoice.getSupplier().getId(), invoice.getSupplier().getName())),
                        invoice.getBalanceDue(),
                        invoice.getDueDate(),
                        today
                ));
        return new ArrayList<>(rows.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountsReceivableInvoiceDto> getAccountsReceivableInvoices() {
        return accountsReceivableInvoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(AccountsReceivableInvoice::getInvoiceDate).reversed()
                        .thenComparing(AccountsReceivableInvoice::getInvoiceNumber).reversed())
                .map(this::mapAccountsReceivableInvoice)
                .toList();
    }

    @Override
    @Transactional
    public AccountsReceivableInvoiceDto createAccountsReceivableInvoice(CreateAccountsReceivableInvoiceRequest request) {
        if (request.getCustomerId() == null && request.getSalesOrderId() == null) {
            throw new BadRequestException("Customer or sales order is required");
        }

        SalesOrder salesOrder = request.getSalesOrderId() == null ? null : salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", request.getSalesOrderId()));
        Customer customer = request.getCustomerId() != null
                ? customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()))
                : salesOrder.getCustomer();
        if (salesOrder != null && !salesOrder.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Sales order customer does not match the selected customer");
        }

        BigDecimal totalAmount = scale(request.getTotalAmount() != null ? request.getTotalAmount() : salesOrder == null ? null : salesOrder.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Accounts receivable invoice amount must be greater than zero");
        }

        LocalDate invoiceDate = request.getInvoiceDate() == null ? LocalDate.now() : request.getInvoiceDate();
        AccountsReceivableInvoice invoice = new AccountsReceivableInvoice();
        invoice.setInvoiceNumber(generateDocumentNumber("ARV"));
        invoice.setCustomerInvoiceNumber(blankToNull(request.getCustomerInvoiceNumber()));
        invoice.setCustomer(customer);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.getDueDate() == null ? invoiceDate.plusDays(30) : request.getDueDate());
        invoice.setCurrency(defaultCurrency(request.getCurrency() != null ? request.getCurrency() : salesOrder == null ? null : salesOrder.getCurrency()));
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        invoice.setBalanceDue(totalAmount);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setNotes(blankToNull(request.getNotes()));
        invoice = accountsReceivableInvoiceRepository.save(invoice);

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_RECEIVABLE_JOURNAL_CODE, "Accounts Receivable Journal", "System journal for AR invoice and receipt posting"),
                "AR_INVOICE",
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts receivable invoice " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                List.of(
                        new JournalLineSpec("ASSET-AR-TRADE", "Trade accounts receivable", totalAmount, BigDecimal.ZERO),
                        new JournalLineSpec("REVENUE-SALES", "Sales revenue", BigDecimal.ZERO, totalAmount)
                )
        );

        return mapAccountsReceivableInvoice(invoice);
    }

    @Override
    @Transactional
    public AccountsReceivableInvoiceDto recordAccountsReceivablePayment(UUID invoiceId, RecordAccountsReceivablePaymentRequest request) {
        AccountsReceivableInvoice invoice = accountsReceivableInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountsReceivableInvoice", "id", invoiceId));
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Payment cannot be recorded for a closed accounts receivable invoice");
        }

        BigDecimal amount = scale(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Receipt amount must be greater than zero");
        }
        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new BadRequestException("Receipt amount cannot exceed the invoice balance due");
        }

        AccountsReceivablePayment payment = new AccountsReceivablePayment();
        payment.setInvoice(invoice);
        payment.setPaymentDate(request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate());
        payment.setAmount(amount);
        payment.setPaymentMethod(blankToNull(request.getPaymentMethod()));
        payment.setPaymentReference(blankToNull(request.getPaymentReference()));
        payment.setNotes(blankToNull(request.getNotes()));
        payment = accountsReceivablePaymentRepository.save(payment);
        invoice.getPayments().add(payment);
        applyPaymentToInvoice(invoice, amount);
        invoice = accountsReceivableInvoiceRepository.save(invoice);

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_RECEIVABLE_JOURNAL_CODE, "Accounts Receivable Journal", "System journal for AR invoice and receipt posting"),
                "AR_RECEIPT",
                payment.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts receivable receipt for " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                List.of(
                        new JournalLineSpec("ASSET-CASH", "Cash receipt", amount, BigDecimal.ZERO),
                        new JournalLineSpec("ASSET-AR-TRADE", "Trade accounts receivable settlement", BigDecimal.ZERO, amount)
                )
        );

        return mapAccountsReceivableInvoice(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingSummaryRowDto> getAccountsReceivableAging() {
        Map<UUID, AgingSummaryRowDto> rows = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        accountsReceivableInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(invoice -> invoice.getCustomer().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(invoice -> applyAging(
                        rows.computeIfAbsent(invoice.getCustomer().getId(), ignored -> createAgingRow(invoice.getCustomer().getId(), invoice.getCustomer().getName())),
                        invoice.getBalanceDue(),
                        invoice.getDueDate(),
                        today
                ));
        return new ArrayList<>(rows.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreasuryAccountDto> getTreasuryAccounts() {
        return treasuryAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(TreasuryAccount::getAccountCode))
                .map(this::mapTreasuryAccount)
                .toList();
    }

    @Override
    @Transactional
    public TreasuryAccountDto createTreasuryAccount(CreateTreasuryAccountRequest request) {
        if (!StringUtils.hasText(request.getAccountCode()) || !StringUtils.hasText(request.getAccountName()) || request.getAccountType() == null) {
            throw new BadRequestException("Treasury account code, name, and type are required");
        }
        String accountCode = request.getAccountCode().trim().toUpperCase(Locale.ROOT);
        if (treasuryAccountRepository.findByAccountCode(accountCode).isPresent()) {
            throw new BadRequestException("Treasury account code already exists");
        }

        TreasuryAccount account = new TreasuryAccount();
        account.setAccountCode(accountCode);
        account.setAccountName(request.getAccountName().trim());
        account.setAccountType(request.getAccountType());
        account.setCurrency(defaultCurrency(request.getCurrency()));
        account.setActive(request.getActive() == null || request.getActive());
        account.setNotes(blankToNull(request.getNotes()));
        return mapTreasuryAccount(treasuryAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreasuryReconciliationDto> getTreasuryReconciliations() {
        return treasuryReconciliationRepository.findAll().stream()
                .sorted(Comparator.comparing(TreasuryReconciliation::getBusinessDate).reversed()
                        .thenComparing(reconciliation -> reconciliation.getTreasuryAccount().getAccountCode()))
                .map(this::mapTreasuryReconciliation)
                .toList();
    }

    @Override
    @Transactional
    public TreasuryReconciliationDto createTreasuryReconciliation(CreateTreasuryReconciliationRequest request) {
        if (request.getTreasuryAccountId() == null) {
            throw new BadRequestException("Treasury account is required");
        }
        TreasuryAccount treasuryAccount = treasuryAccountRepository.findById(request.getTreasuryAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("TreasuryAccount", "id", request.getTreasuryAccountId()));
        if (!treasuryAccount.isActive()) {
            throw new BadRequestException("Selected treasury account is inactive");
        }

        LocalDate businessDate = request.getBusinessDate() == null ? LocalDate.now() : request.getBusinessDate();
        BigDecimal statementBalance = scale(request.getStatementBalance());
        TreasuryReconciliation reconciliation = new TreasuryReconciliation();
        reconciliation.setTreasuryAccount(treasuryAccount);
        reconciliation.setBusinessDate(businessDate);
        reconciliation.setStatus(ReconciliationStatus.OPEN);
        reconciliation.setStatementBalance(statementBalance);
        reconciliation.setNotes(blankToNull(request.getNotes()));

        List<ReconciliationLineSeed> seeds = buildReconciliationSeeds(treasuryAccount, businessDate);
        BigDecimal systemBalance = seeds.stream()
                .map(ReconciliationLineSeed::amount)
                .reduce(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), BigDecimal::add);
        reconciliation.setSystemBalance(scale(systemBalance));
        reconciliation.setDifferenceAmount(scale(statementBalance.subtract(systemBalance)));
        reconciliation = treasuryReconciliationRepository.save(reconciliation);

        for (ReconciliationLineSeed seed : seeds) {
            TreasuryReconciliationLine line = new TreasuryReconciliationLine();
            line.setReconciliation(reconciliation);
            line.setSourceType(seed.sourceType());
            line.setSourceId(seed.sourceId());
            line.setSourceReference(seed.sourceReference());
            line.setTransactionDate(seed.transactionDate());
            line.setDescription(seed.description());
            line.setAmount(scale(seed.amount()));
            line.setMatched(true);
            reconciliation.getLines().add(line);
        }
        reconciliation = treasuryReconciliationRepository.save(reconciliation);
        return mapTreasuryReconciliation(reconciliation);
    }

    @Override
    @Transactional
    public TreasuryReconciliationDto completeTreasuryReconciliation(UUID reconciliationId, CompleteTreasuryReconciliationRequest request) {
        TreasuryReconciliation reconciliation = treasuryReconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("TreasuryReconciliation", "id", reconciliationId));
        if (reconciliation.getStatus() == ReconciliationStatus.COMPLETED) {
            return mapTreasuryReconciliation(reconciliation);
        }

        if (request != null && StringUtils.hasText(request.getNotes())) {
            reconciliation.setNotes(request.getNotes().trim());
        }
        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        reconciliation.setCompletedAt(LocalDateTime.now());
        return mapTreasuryReconciliation(treasuryReconciliationRepository.save(reconciliation));
    }

    @Override
    @Transactional
    public JournalEntryDto createManualJournalEntry(CreateManualJournalEntryRequest request) {
        if (request.getJournalId() == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BadRequestException("Journal and at least one line are required");
        }

        AccountingJournal journal = accountingJournalRepository.findById(request.getJournalId())
                .orElseThrow(() -> new ResourceNotFoundException("AccountingJournal", "id", request.getJournalId()));
        if (!journal.isActive()) {
            throw new BadRequestException("Selected journal is inactive");
        }

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber());
        entry.setJournal(journal);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setEntryDate(request.getEntryDate() == null ? LocalDateTime.now() : request.getEntryDate());
        entry.setSourceDocumentType("MANUAL_JOURNAL");
        entry.setSourceDocumentId(entry.getEntryNumber());
        entry.setSourceDocumentNumber(entry.getEntryNumber());
        entry.setMemo(blankToNull(request.getMemo()));
        entry.setCurrency(defaultCurrency(request.getCurrency()));

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int lineNumber = 1;
        for (CreateManualJournalEntryRequest.LineRequest lineRequest : request.getLines()) {
            ChartOfAccount account = chartOfAccountRepository.findById(lineRequest.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", lineRequest.getAccountId()));
            if (!account.isActive()) {
                throw new BadRequestException("Journal line account is inactive: " + account.getAccountCode());
            }
            if (!account.isAllowManualPosting()) {
                throw new BadRequestException("Manual posting is not allowed to account: " + account.getAccountCode());
            }

            BigDecimal debit = scale(lineRequest.getDebitAmount());
            BigDecimal credit = scale(lineRequest.getCreditAmount());
            if ((debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0)
                    || (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0)) {
                throw new BadRequestException("Each journal line must have either a debit or a credit amount");
            }

            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(entry);
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(blankToNull(lineRequest.getDescription()));
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            entry.getLines().add(line);

            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Manual journal entry must be balanced");
        }

        entry.setTotalDebits(scale(totalDebits));
        entry.setTotalCredits(scale(totalCredits));
        entry.setPostedAt(LocalDateTime.now());
        return mapEntry(journalEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public JournalEntryDto postFinancialEvent(UUID financialEventId) {
        FinancialEvent event = financialEventRepository.findById(financialEventId)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialEvent", "id", financialEventId));

        if (event.getPostingStatus() != PostingStatus.PENDING) {
            throw new BadRequestException("Only pending financial events can be posted");
        }

        JournalEntry existing = journalEntryRepository.findByFinancialEventId(financialEventId).orElse(null);
        if (existing != null && existing.getStatus() == JournalEntryStatus.POSTED) {
            return mapEntry(existing);
        }

        if (event.getSubledgerEntries().isEmpty()) {
            throw new BadRequestException("Financial event has no subledger lines");
        }

        AccountingJournal journal = ensureSystemJournal();
        JournalEntry entry = existing != null ? existing : new JournalEntry();
        entry.setEntryNumber(existing != null ? existing.getEntryNumber() : generateEntryNumber());
        entry.setJournal(journal);
        entry.setFinancialEvent(event);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setEntryDate(LocalDateTime.now());
        entry.setSourceDocumentType(event.getSourceDocumentType());
        entry.setSourceDocumentId(event.getSourceDocumentId());
        entry.setSourceDocumentNumber(event.getSourceDocumentNumber());
        entry.setMemo(event.getSummary());
        entry.setCurrency(event.getCurrency());
        entry.getLines().clear();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int lineNumber = 1;
        for (SubledgerEntry subledgerEntry : event.getSubledgerEntries()) {
            ChartOfAccount account = ensureAccount(subledgerEntry.getAccountCode(), subledgerEntry.getAccountName());

            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(entry);
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(subledgerEntry.getDescription());
            if (subledgerEntry.getEntryType() == SubledgerEntryType.DEBIT) {
                line.setDebitAmount(scale(subledgerEntry.getAmount()));
                line.setCreditAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
                totalDebits = totalDebits.add(line.getDebitAmount());
            } else {
                line.setDebitAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
                line.setCreditAmount(scale(subledgerEntry.getAmount()));
                totalCredits = totalCredits.add(line.getCreditAmount());
            }
            entry.getLines().add(line);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Financial event is not balanced and cannot be posted");
        }

        entry.setTotalDebits(scale(totalDebits));
        entry.setTotalCredits(scale(totalCredits));
        entry.setPostedAt(LocalDateTime.now());
        entry = journalEntryRepository.save(entry);

        event.setPostingStatus(PostingStatus.POSTED);
        event.getSubledgerEntries().forEach(line -> line.setPostingStatus(PostingStatus.POSTED));
        financialEventRepository.save(event);

        return mapEntry(entry);
    }

    @Override
    @Transactional
    public List<JournalEntryDto> postPendingFinancialEvents() {
        List<FinancialEvent> events = financialEventRepository.findAll().stream()
                .filter(event -> event.getPostingStatus() == PostingStatus.PENDING)
                .sorted(Comparator.comparing(FinancialEvent::getOccurredAt))
                .toList();

        List<JournalEntryDto> results = new ArrayList<>();
        for (FinancialEvent event : events) {
            results.add(postFinancialEvent(event.getId()));
        }
        return results;
    }

    @Override
    @Transactional
    public JournalEntryDto reverseJournalEntry(UUID journalEntryId, String memo) {
        JournalEntry original = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", "id", journalEntryId));
        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new BadRequestException("Only posted journal entries can be reversed");
        }
        if (original.getReversalOfEntry() != null) {
            throw new BadRequestException("Reversal journal entries cannot be reversed");
        }
        if (journalEntryRepository.existsByReversalOfEntryId(journalEntryId)) {
            throw new BadRequestException("Journal entry has already been reversed");
        }

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNumber(generateEntryNumber());
        reversal.setJournal(original.getJournal());
        reversal.setStatus(JournalEntryStatus.POSTED);
        reversal.setEntryDate(LocalDateTime.now());
        reversal.setSourceDocumentType("JOURNAL_REVERSAL");
        reversal.setSourceDocumentId(original.getId().toString());
        reversal.setSourceDocumentNumber(original.getEntryNumber());
        reversal.setMemo(blankToNull(memo) == null ? "Reversal of " + original.getEntryNumber() : blankToNull(memo));
        reversal.setCurrency(original.getCurrency());
        reversal.setReversalOfEntry(original);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int lineNumber = 1;
        for (JournalEntryLine originalLine : original.getLines()) {
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(reversal);
            line.setAccount(originalLine.getAccount());
            line.setLineNumber(lineNumber++);
            line.setDescription("Reversal: " + (originalLine.getDescription() == null ? original.getEntryNumber() : originalLine.getDescription()));
            line.setDebitAmount(scale(originalLine.getCreditAmount()));
            line.setCreditAmount(scale(originalLine.getDebitAmount()));
            reversal.getLines().add(line);
            totalDebits = totalDebits.add(line.getDebitAmount());
            totalCredits = totalCredits.add(line.getCreditAmount());
        }

        reversal.setTotalDebits(scale(totalDebits));
        reversal.setTotalCredits(scale(totalCredits));
        reversal.setPostedAt(LocalDateTime.now());
        original.setStatus(JournalEntryStatus.REVERSED);
        journalEntryRepository.save(original);
        return mapEntry(journalEntryRepository.save(reversal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrialBalanceRowDto> getTrialBalance() {
        return chartOfAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(ChartOfAccount::getAccountCode))
                .map(account -> {
                    BigDecimal debits = scale(journalEntryLineRepository.sumDebitsByAccount(account.getId()));
                    BigDecimal credits = scale(journalEntryLineRepository.sumCreditsByAccount(account.getId()));
                    if (debits.compareTo(BigDecimal.ZERO) == 0 && credits.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }
                    TrialBalanceRowDto dto = new TrialBalanceRowDto();
                    dto.setAccountId(account.getId());
                    dto.setAccountCode(account.getAccountCode());
                    dto.setAccountName(account.getAccountName());
                    dto.setAccountType(account.getAccountType());
                    dto.setTotalDebits(debits);
                    dto.setTotalCredits(credits);
                    dto.setBalance(scale(debits.subtract(credits)));
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialStatementRowDto> getProfitAndLoss() {
        return chartOfAccountRepository.findAll().stream()
                .filter(account -> account.getAccountType() == AccountType.REVENUE || account.getAccountType() == AccountType.EXPENSE)
                .sorted(Comparator.comparing(ChartOfAccount::getAccountCode))
                .map(this::mapStatementRow)
                .filter(row -> row.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialStatementRowDto> getBalanceSheet() {
        return chartOfAccountRepository.findAll().stream()
                .filter(account -> account.getAccountType() == AccountType.ASSET
                        || account.getAccountType() == AccountType.LIABILITY
                        || account.getAccountType() == AccountType.EQUITY)
                .sorted(Comparator.comparing(ChartOfAccount::getAccountCode))
                .map(this::mapStatementRow)
                .filter(row -> row.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    private AccountingJournal ensureSystemJournal() {
        return ensureSystemJournal(SYSTEM_JOURNAL_CODE, "System Posting Journal",
                "System-generated journal for inventory and accounting event posting");
    }

    private AccountingJournal ensureSystemJournal(String journalCode, String journalName, String description) {
        return accountingJournalRepository.findByJournalCode(journalCode)
                .orElseGet(() -> {
                    AccountingJournal journal = new AccountingJournal();
                    journal.setJournalCode(journalCode);
                    journal.setJournalName(journalName);
                    journal.setDescription(description);
                    journal.setSystemJournal(true);
                    journal.setActive(true);
                    return accountingJournalRepository.save(journal);
                });
    }

    private ChartOfAccount ensureAccount(String accountCode, String accountName) {
        return chartOfAccountRepository.findByAccountCode(accountCode)
                .orElseGet(() -> {
                    ChartOfAccount account = new ChartOfAccount();
                    account.setAccountCode(accountCode);
                    account.setAccountName(accountName);
                    account.setAccountType(resolveAccountType(accountCode));
                    account.setAllowManualPosting(false);
                    account.setActive(true);
                    account.setDescription("Auto-provisioned from accounting event posting");
                    return chartOfAccountRepository.save(account);
                });
    }

    private AccountType resolveAccountType(String accountCode) {
        String code = accountCode == null ? "" : accountCode.toUpperCase(Locale.ROOT);
        if (code.contains("REVENUE") || code.contains("RETURNS")) {
            return AccountType.REVENUE;
        }
        if (code.contains("EXPENSE") || code.contains("COGS") || code.contains("LOSS") || code.contains("WRITE_OFF")) {
            return AccountType.EXPENSE;
        }
        if (code.contains("LIABILITY") || code.contains("ACCRUAL") || code.contains("CLEARING")) {
            return AccountType.LIABILITY;
        }
        if (code.contains("EQUITY")) {
            return AccountType.EQUITY;
        }
        return AccountType.ASSET;
    }

    private JournalEntry createPostedJournalEntry(AccountingJournal journal,
                                                  String sourceDocumentType,
                                                  String sourceDocumentId,
                                                  String sourceDocumentNumber,
                                                  String memo,
                                                  String currency,
                                                  List<JournalLineSpec> lines) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber());
        entry.setJournal(journal);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setEntryDate(LocalDateTime.now());
        entry.setSourceDocumentType(sourceDocumentType);
        entry.setSourceDocumentId(sourceDocumentId);
        entry.setSourceDocumentNumber(sourceDocumentNumber);
        entry.setMemo(memo);
        entry.setCurrency(defaultCurrency(currency));

        BigDecimal totalDebits = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        int lineNumber = 1;
        for (JournalLineSpec spec : lines) {
            ChartOfAccount account = ensureAccount(spec.accountCode(), spec.accountName());
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(entry);
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(spec.accountName());
            line.setDebitAmount(scale(spec.debitAmount()));
            line.setCreditAmount(scale(spec.creditAmount()));
            totalDebits = totalDebits.add(line.getDebitAmount());
            totalCredits = totalCredits.add(line.getCreditAmount());
            entry.getLines().add(line);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Journal entry must be balanced");
        }

        entry.setTotalDebits(scale(totalDebits));
        entry.setTotalCredits(scale(totalCredits));
        entry.setPostedAt(LocalDateTime.now());
        return journalEntryRepository.save(entry);
    }

    private void applyPaymentToInvoice(AccountsPayableInvoice invoice, BigDecimal amount) {
        invoice.setPaidAmount(scale(invoice.getPaidAmount().add(amount)));
        invoice.setBalanceDue(scale(invoice.getTotalAmount().subtract(invoice.getPaidAmount())));
        invoice.setStatus(invoice.getBalanceDue().compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
    }

    private void applyPaymentToInvoice(AccountsReceivableInvoice invoice, BigDecimal amount) {
        invoice.setPaidAmount(scale(invoice.getPaidAmount().add(amount)));
        invoice.setBalanceDue(scale(invoice.getTotalAmount().subtract(invoice.getPaidAmount())));
        invoice.setStatus(invoice.getBalanceDue().compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
    }

    private AgingSummaryRowDto createAgingRow(UUID partyId, String partyName) {
        AgingSummaryRowDto row = new AgingSummaryRowDto();
        row.setPartyId(partyId);
        row.setPartyName(partyName);
        row.setInvoiceCount(0);
        row.setTotalOpenAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        row.setCurrentAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        row.setDays1To30Amount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        row.setDays31To60Amount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        row.setDays61To90Amount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        row.setOver90Amount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        return row;
    }

    private void applyAging(AgingSummaryRowDto row, BigDecimal balance, LocalDate dueDate, LocalDate today) {
        BigDecimal openAmount = scale(balance);
        row.setInvoiceCount(row.getInvoiceCount() + 1);
        row.setTotalOpenAmount(scale(row.getTotalOpenAmount().add(openAmount)));

        long daysPastDue = dueDate == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
        if (daysPastDue <= 0) {
            row.setCurrentAmount(scale(row.getCurrentAmount().add(openAmount)));
        } else if (daysPastDue <= 30) {
            row.setDays1To30Amount(scale(row.getDays1To30Amount().add(openAmount)));
        } else if (daysPastDue <= 60) {
            row.setDays31To60Amount(scale(row.getDays31To60Amount().add(openAmount)));
        } else if (daysPastDue <= 90) {
            row.setDays61To90Amount(scale(row.getDays61To90Amount().add(openAmount)));
        } else {
            row.setOver90Amount(scale(row.getOver90Amount().add(openAmount)));
        }
    }

    private List<ReconciliationLineSeed> buildReconciliationSeeds(TreasuryAccount treasuryAccount, LocalDate businessDate) {
        List<ReconciliationLineSeed> seeds = new ArrayList<>();

        if (treasuryAccount.getAccountType() == TreasuryAccountType.CASH) {
            posShiftRepository.findAll().stream()
                    .filter(shift -> shift.getClosedAt() != null && businessDate.equals(shift.getClosedAt().toLocalDate()))
                    .filter(shift -> shift.getDeclaredCashAmount() != null && shift.getDeclaredCashAmount().compareTo(BigDecimal.ZERO) != 0)
                    .map(shift -> new ReconciliationLineSeed(
                            ReconciliationSourceType.POS_SHIFT,
                            shift.getId().toString(),
                            shift.getTerminal().getTerminalCode(),
                            shift.getClosedAt().toLocalDate(),
                            "POS shift cash settlement for " + shift.getTerminal().getName(),
                            scale(shift.getDeclaredCashAmount())
                    ))
                    .forEach(seeds::add);
        }

        accountsReceivablePaymentRepository.findAll().stream()
                .filter(payment -> payment.getPaymentDate() != null && businessDate.equals(payment.getPaymentDate()))
                .filter(payment -> treasuryMatchesPaymentMethod(treasuryAccount.getAccountType(), payment.getPaymentMethod()))
                .map(payment -> new ReconciliationLineSeed(
                        ReconciliationSourceType.ACCOUNTS_RECEIVABLE_PAYMENT,
                        payment.getId().toString(),
                        payment.getInvoice().getInvoiceNumber(),
                        payment.getPaymentDate(),
                        "AR receipt from " + payment.getInvoice().getCustomer().getName(),
                        scale(payment.getAmount())
                ))
                .forEach(seeds::add);

        accountsPayablePaymentRepository.findAll().stream()
                .filter(payment -> payment.getPaymentDate() != null && businessDate.equals(payment.getPaymentDate()))
                .filter(payment -> treasuryMatchesPaymentMethod(treasuryAccount.getAccountType(), payment.getPaymentMethod()))
                .map(payment -> new ReconciliationLineSeed(
                        ReconciliationSourceType.ACCOUNTS_PAYABLE_PAYMENT,
                        payment.getId().toString(),
                        payment.getInvoice().getInvoiceNumber(),
                        payment.getPaymentDate(),
                        "AP payment to " + payment.getInvoice().getSupplier().getName(),
                        scale(payment.getAmount()).negate()
                ))
                .forEach(seeds::add);

        return seeds.stream()
                .filter(seed -> seed.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(ReconciliationLineSeed::transactionDate).thenComparing(ReconciliationLineSeed::sourceReference))
                .toList();
    }

    private boolean treasuryMatchesPaymentMethod(TreasuryAccountType accountType, String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (accountType == TreasuryAccountType.CASH) {
            return Objects.equals(method, "CASH");
        }
        if (accountType == TreasuryAccountType.WALLET) {
            return Objects.equals(method, "WALLET");
        }
        if (accountType == TreasuryAccountType.CLEARING) {
            return Objects.equals(method, "OTHER");
        }
        return !Objects.equals(method, "CASH") && !Objects.equals(method, "WALLET");
    }

    private String generateEntryNumber() {
        return "JE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String generateDocumentNumber(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : value.setScale(6, RoundingMode.HALF_UP);
    }

    private String defaultCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ChartOfAccountDto mapAccount(ChartOfAccount account) {
        ChartOfAccountDto dto = new ChartOfAccountDto();
        dto.setId(account.getId());
        dto.setAccountCode(account.getAccountCode());
        dto.setAccountName(account.getAccountName());
        dto.setAccountType(account.getAccountType());
        dto.setParentAccountId(account.getParentAccount() == null ? null : account.getParentAccount().getId());
        dto.setParentAccountCode(account.getParentAccount() == null ? null : account.getParentAccount().getAccountCode());
        dto.setAllowManualPosting(account.isAllowManualPosting());
        dto.setActive(account.isActive());
        dto.setDescription(account.getDescription());
        return dto;
    }

    private AccountingJournalDto mapJournal(AccountingJournal journal) {
        AccountingJournalDto dto = new AccountingJournalDto();
        dto.setId(journal.getId());
        dto.setJournalCode(journal.getJournalCode());
        dto.setJournalName(journal.getJournalName());
        dto.setDescription(journal.getDescription());
        dto.setSystemJournal(journal.isSystemJournal());
        dto.setActive(journal.isActive());
        return dto;
    }

    private JournalEntryDto mapEntry(JournalEntry entry) {
        JournalEntryDto dto = new JournalEntryDto();
        dto.setId(entry.getId());
        dto.setEntryNumber(entry.getEntryNumber());
        dto.setJournalId(entry.getJournal().getId());
        dto.setJournalCode(entry.getJournal().getJournalCode());
        dto.setJournalName(entry.getJournal().getJournalName());
        dto.setFinancialEventId(entry.getFinancialEvent() == null ? null : entry.getFinancialEvent().getId());
        dto.setReversalOfEntryId(entry.getReversalOfEntry() == null ? null : entry.getReversalOfEntry().getId());
        dto.setStatus(entry.getStatus());
        dto.setEntryDate(entry.getEntryDate());
        dto.setSourceDocumentType(entry.getSourceDocumentType());
        dto.setSourceDocumentId(entry.getSourceDocumentId());
        dto.setSourceDocumentNumber(entry.getSourceDocumentNumber());
        dto.setMemo(entry.getMemo());
        dto.setCurrency(entry.getCurrency());
        dto.setTotalDebits(entry.getTotalDebits());
        dto.setTotalCredits(entry.getTotalCredits());
        dto.setPostedAt(entry.getPostedAt());
        dto.setLines(entry.getLines().stream().sorted(Comparator.comparing(JournalEntryLine::getLineNumber)).map(this::mapLine).toList());
        return dto;
    }

    private JournalEntryLineDto mapLine(JournalEntryLine line) {
        JournalEntryLineDto dto = new JournalEntryLineDto();
        dto.setId(line.getId());
        dto.setLineNumber(line.getLineNumber());
        dto.setAccountId(line.getAccount().getId());
        dto.setAccountCode(line.getAccount().getAccountCode());
        dto.setAccountName(line.getAccount().getAccountName());
        dto.setDescription(line.getDescription());
        dto.setDebitAmount(line.getDebitAmount());
        dto.setCreditAmount(line.getCreditAmount());
        return dto;
    }

    private AccountsPayableInvoiceDto mapAccountsPayableInvoice(AccountsPayableInvoice invoice) {
        AccountsPayableInvoiceDto dto = new AccountsPayableInvoiceDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setSupplierInvoiceNumber(invoice.getSupplierInvoiceNumber());
        dto.setSupplierId(invoice.getSupplier().getId());
        dto.setSupplierName(invoice.getSupplier().getName());
        dto.setPurchaseOrderId(invoice.getPurchaseOrder() == null ? null : invoice.getPurchaseOrder().getId());
        dto.setPurchaseOrderNumber(invoice.getPurchaseOrder() == null ? null : invoice.getPurchaseOrder().getPoNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setCurrency(invoice.getCurrency());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setBalanceDue(invoice.getBalanceDue());
        dto.setStatus(invoice.getStatus());
        dto.setNotes(invoice.getNotes());
        dto.setPayments(invoice.getPayments().stream()
                .sorted(Comparator.comparing(AccountsPayablePayment::getPaymentDate).reversed())
                .map(this::mapAccountsPayablePayment)
                .toList());
        return dto;
    }

    private AccountsPayablePaymentDto mapAccountsPayablePayment(AccountsPayablePayment payment) {
        AccountsPayablePaymentDto dto = new AccountsPayablePaymentDto();
        dto.setId(payment.getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setNotes(payment.getNotes());
        return dto;
    }

    private AccountsReceivableInvoiceDto mapAccountsReceivableInvoice(AccountsReceivableInvoice invoice) {
        AccountsReceivableInvoiceDto dto = new AccountsReceivableInvoiceDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerInvoiceNumber(invoice.getCustomerInvoiceNumber());
        dto.setCustomerId(invoice.getCustomer().getId());
        dto.setCustomerName(invoice.getCustomer().getName());
        dto.setSalesOrderId(invoice.getSalesOrder() == null ? null : invoice.getSalesOrder().getId());
        dto.setSalesOrderNumber(invoice.getSalesOrder() == null ? null : invoice.getSalesOrder().getSoNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setCurrency(invoice.getCurrency());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setBalanceDue(invoice.getBalanceDue());
        dto.setStatus(invoice.getStatus());
        dto.setNotes(invoice.getNotes());
        dto.setPayments(invoice.getPayments().stream()
                .sorted(Comparator.comparing(AccountsReceivablePayment::getPaymentDate).reversed())
                .map(this::mapAccountsReceivablePayment)
                .toList());
        return dto;
    }

    private AccountsReceivablePaymentDto mapAccountsReceivablePayment(AccountsReceivablePayment payment) {
        AccountsReceivablePaymentDto dto = new AccountsReceivablePaymentDto();
        dto.setId(payment.getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setNotes(payment.getNotes());
        return dto;
    }

    private TreasuryAccountDto mapTreasuryAccount(TreasuryAccount account) {
        TreasuryAccountDto dto = new TreasuryAccountDto();
        dto.setId(account.getId());
        dto.setAccountCode(account.getAccountCode());
        dto.setAccountName(account.getAccountName());
        dto.setAccountType(account.getAccountType());
        dto.setCurrency(account.getCurrency());
        dto.setActive(account.isActive());
        dto.setNotes(account.getNotes());
        return dto;
    }

    private TreasuryReconciliationDto mapTreasuryReconciliation(TreasuryReconciliation reconciliation) {
        TreasuryReconciliationDto dto = new TreasuryReconciliationDto();
        dto.setId(reconciliation.getId());
        dto.setTreasuryAccountId(reconciliation.getTreasuryAccount().getId());
        dto.setTreasuryAccountCode(reconciliation.getTreasuryAccount().getAccountCode());
        dto.setTreasuryAccountName(reconciliation.getTreasuryAccount().getAccountName());
        dto.setBusinessDate(reconciliation.getBusinessDate());
        dto.setStatus(reconciliation.getStatus());
        dto.setStatementBalance(reconciliation.getStatementBalance());
        dto.setSystemBalance(reconciliation.getSystemBalance());
        dto.setDifferenceAmount(reconciliation.getDifferenceAmount());
        dto.setNotes(reconciliation.getNotes());
        dto.setCompletedAt(reconciliation.getCompletedAt());
        dto.setLines(reconciliation.getLines().stream()
                .sorted(Comparator.comparing(TreasuryReconciliationLine::getTransactionDate).thenComparing(TreasuryReconciliationLine::getSourceReference))
                .map(this::mapTreasuryReconciliationLine)
                .toList());
        return dto;
    }

    private TreasuryReconciliationLineDto mapTreasuryReconciliationLine(TreasuryReconciliationLine line) {
        TreasuryReconciliationLineDto dto = new TreasuryReconciliationLineDto();
        dto.setId(line.getId());
        dto.setSourceType(line.getSourceType());
        dto.setSourceId(line.getSourceId());
        dto.setSourceReference(line.getSourceReference());
        dto.setTransactionDate(line.getTransactionDate());
        dto.setDescription(line.getDescription());
        dto.setAmount(line.getAmount());
        dto.setMatched(line.isMatched());
        return dto;
    }

    private FinancialStatementRowDto mapStatementRow(ChartOfAccount account) {
        BigDecimal debits = scale(journalEntryLineRepository.sumDebitsByAccount(account.getId()));
        BigDecimal credits = scale(journalEntryLineRepository.sumCreditsByAccount(account.getId()));

        FinancialStatementRowDto dto = new FinancialStatementRowDto();
        dto.setAccountId(account.getId());
        dto.setAccountCode(account.getAccountCode());
        dto.setAccountName(account.getAccountName());
        dto.setAccountType(account.getAccountType());

        BigDecimal amount = switch (account.getAccountType()) {
            case ASSET, EXPENSE -> debits.subtract(credits);
            case LIABILITY, EQUITY, REVENUE -> credits.subtract(debits);
        };
        dto.setAmount(scale(amount));
        return dto;
    }

    private record JournalLineSpec(String accountCode, String accountName, BigDecimal debitAmount, BigDecimal creditAmount) {
    }

    private record ReconciliationLineSeed(ReconciliationSourceType sourceType,
                                          String sourceId,
                                          String sourceReference,
                                          LocalDate transactionDate,
                                          String description,
                                          BigDecimal amount) {
    }
}
