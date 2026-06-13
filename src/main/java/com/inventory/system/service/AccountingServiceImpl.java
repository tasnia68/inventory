package com.inventory.system.service;

import com.inventory.system.accounting.api.event.AccountingSourceDocumentPort;
import com.inventory.system.common.entity.AccountType;
import com.inventory.system.common.entity.AccountingAuditLog;
import com.inventory.system.common.entity.AccountingJournal;
import com.inventory.system.common.entity.AccountsPayableInvoice;
import com.inventory.system.common.entity.AccountsPayablePayment;
import com.inventory.system.common.entity.AccountsReceivableInvoice;
import com.inventory.system.common.entity.AccountsReceivablePayment;
import com.inventory.system.common.entity.ChartOfAccount;
import com.inventory.system.common.entity.FinancialEvent;
import com.inventory.system.common.entity.InvoiceStatus;
import com.inventory.system.common.entity.JournalEntry;
import com.inventory.system.common.entity.JournalEntryAttachment;
import com.inventory.system.common.entity.JournalEntryLine;
import com.inventory.system.common.entity.JournalEntryStatus;
import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.common.entity.ReconciliationSourceType;
import com.inventory.system.common.entity.ReconciliationStatus;
import com.inventory.system.common.entity.RecurringJournalCadence;
import com.inventory.system.common.entity.RecurringJournalTemplate;
import com.inventory.system.common.entity.RecurringJournalTemplateLine;
import com.inventory.system.common.entity.SubledgerEntry;
import com.inventory.system.common.entity.SubledgerEntryType;
import com.inventory.system.common.entity.TaxRate;
import com.inventory.system.common.entity.TreasuryAccount;
import com.inventory.system.common.entity.TreasuryAccountType;
import com.inventory.system.common.entity.TreasuryReconciliation;
import com.inventory.system.common.entity.TreasuryReconciliationLine;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AccountingJournalDto;
import com.inventory.system.payload.AccountingAuditLogDto;
import com.inventory.system.payload.AccountLedgerDto;
import com.inventory.system.payload.AccountLedgerLineDto;
import com.inventory.system.payload.AccountsPayableInvoiceDto;
import com.inventory.system.payload.AccountsPayablePaymentDto;
import com.inventory.system.payload.AccountsReceivableInvoiceDto;
import com.inventory.system.payload.AccountsReceivablePaymentDto;
import com.inventory.system.payload.AgingSummaryRowDto;
import com.inventory.system.payload.ChartOfAccountDto;
import com.inventory.system.payload.CompleteTreasuryReconciliationRequest;
import com.inventory.system.payload.CashFlowRowDto;
import com.inventory.system.payload.CreateAccountsPayableInvoiceRequest;
import com.inventory.system.payload.CreateAccountsReceivableInvoiceRequest;
import com.inventory.system.payload.CreateAccountingJournalRequest;
import com.inventory.system.payload.CreateChartOfAccountRequest;
import com.inventory.system.payload.CreateManualJournalEntryRequest;
import com.inventory.system.payload.CreateRecurringJournalTemplateRequest;
import com.inventory.system.payload.CreateTaxRateRequest;
import com.inventory.system.payload.CreateTreasuryAccountRequest;
import com.inventory.system.payload.CreateTreasuryReconciliationRequest;
import com.inventory.system.payload.FinancialStatementRowDto;
import com.inventory.system.payload.FinancialEventDto;
import com.inventory.system.payload.JournalEntryAttachmentDto;
import com.inventory.system.payload.JournalEntryDto;
import com.inventory.system.payload.JournalEntryLineDto;
import com.inventory.system.payload.RecordAccountsPayablePaymentRequest;
import com.inventory.system.payload.RecordAccountsReceivablePaymentRequest;
import com.inventory.system.payload.RecurringJournalTemplateDto;
import com.inventory.system.payload.SubledgerEntryDto;
import com.inventory.system.payload.TaxRateDto;
import com.inventory.system.payload.TreasuryAccountDto;
import com.inventory.system.payload.TreasuryReconciliationDto;
import com.inventory.system.payload.TreasuryReconciliationLineDto;
import com.inventory.system.payload.TrialBalanceRowDto;
import com.inventory.system.payload.VatReturnRowDto;
import com.inventory.system.repository.AccountsPayableInvoiceRepository;
import com.inventory.system.repository.AccountsPayablePaymentRepository;
import com.inventory.system.repository.AccountsReceivableInvoiceRepository;
import com.inventory.system.repository.AccountsReceivablePaymentRepository;
import com.inventory.system.repository.AccountingAuditLogRepository;
import com.inventory.system.repository.AccountingJournalRepository;
import com.inventory.system.repository.ChartOfAccountRepository;
import com.inventory.system.repository.FinancialEventRepository;
import com.inventory.system.repository.JournalEntryAttachmentRepository;
import com.inventory.system.repository.JournalEntryLineRepository;
import com.inventory.system.repository.JournalEntryRepository;
import com.inventory.system.repository.RecurringJournalTemplateRepository;
import com.inventory.system.repository.TaxRateRepository;
import com.inventory.system.repository.TreasuryAccountRepository;
import com.inventory.system.repository.TreasuryReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    private final AccountingAuditLogRepository accountingAuditLogRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountingJournalRepository accountingJournalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryAttachmentRepository journalEntryAttachmentRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final FinancialEventRepository financialEventRepository;
    private final FinancialEventFailureService financialEventFailureService;
    private final TaxRateRepository taxRateRepository;
    private final RecurringJournalTemplateRepository recurringJournalTemplateRepository;
    private final AccountsPayableInvoiceRepository accountsPayableInvoiceRepository;
    private final AccountsPayablePaymentRepository accountsPayablePaymentRepository;
    private final AccountsReceivableInvoiceRepository accountsReceivableInvoiceRepository;
    private final AccountsReceivablePaymentRepository accountsReceivablePaymentRepository;
    private final AccountingSourceDocumentPort sourceDocumentPort;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final TreasuryReconciliationRepository treasuryReconciliationRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountDto> getAccounts() {
        return chartOfAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(ChartOfAccount::getAccountCode))
                .map(this::mapAccount)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingAuditLogDto> getAccountingAuditLog(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        return accountingAuditLogRepository.findAllByOrderByOccurredAtDesc(
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "occurredAt"))
                ).stream()
                .map(this::mapAuditLog)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLedgerDto getAccountLedger(UUID accountId, LocalDate from, LocalDate to, int page, int size) {
        ChartOfAccount account = chartOfAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", accountId));
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ledger from date cannot be after to date");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 500);

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        BigDecimal opening = fromDateTime == null
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : accountBalance(
                        account,
                        journalEntryLineRepository.sumDebitsByAccountBefore(accountId, fromDateTime),
                        journalEntryLineRepository.sumCreditsByAccountBefore(accountId, fromDateTime)
                );

        AccountLedgerDto dto = new AccountLedgerDto();
        dto.setAccountId(account.getId());
        dto.setAccountCode(account.getAccountCode());
        dto.setAccountName(account.getAccountName());
        dto.setAccountType(account.getAccountType());
        dto.setFrom(from);
        dto.setTo(to);
        dto.setPage(safePage);
        dto.setSize(safeSize);
        dto.setOpeningBalance(scale(opening));

        BigDecimal runningBalance = opening;
        List<AccountLedgerLineDto> lines = new ArrayList<>();
        List<JournalEntryLine> ledgerLines = journalEntryLineRepository.findPostedLedgerLines(accountId, fromDateTime, toDateTime);
        dto.setTotalLines(ledgerLines.size());
        int fromIndex = safePage * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, ledgerLines.size());
        for (int index = 0; index < ledgerLines.size(); index++) {
            JournalEntryLine line = ledgerLines.get(index);
            runningBalance = runningBalance.add(lineImpact(account, line));
            if (index < fromIndex || index >= toIndex) {
                continue;
            }
            AccountLedgerLineDto lineDto = new AccountLedgerLineDto();
            JournalEntry entry = line.getJournalEntry();
            lineDto.setLineId(line.getId());
            lineDto.setJournalEntryId(entry.getId());
            lineDto.setEntryNumber(entry.getEntryNumber());
            lineDto.setEntryDate(entry.getEntryDate());
            lineDto.setJournalCode(entry.getJournal().getJournalCode());
            lineDto.setSourceDocumentType(entry.getSourceDocumentType());
            lineDto.setSourceDocumentId(entry.getSourceDocumentId());
            lineDto.setSourceDocumentNumber(entry.getSourceDocumentNumber());
            lineDto.setMemo(entry.getMemo());
            lineDto.setDescription(line.getDescription());
            lineDto.setDebitAmount(scale(line.getDebitAmount()));
            lineDto.setCreditAmount(scale(line.getCreditAmount()));
            lineDto.setRunningBalance(scale(runningBalance));
            lines.add(lineDto);
        }

        dto.setLines(lines);
        dto.setClosingBalance(scale(runningBalance));
        return dto;
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
    public List<TaxRateDto> getTaxRates() {
        return taxRateRepository.findAll().stream()
                .sorted(Comparator.comparing(TaxRate::getCode))
                .map(this::mapTaxRate)
                .toList();
    }

    @Override
    @Transactional
    public TaxRateDto createTaxRate(CreateTaxRateRequest request) {
        if (!StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName()) || request.getRate() == null) {
            throw new BadRequestException("Tax code, name, and rate are required");
        }
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (taxRateRepository.findByCode(code).isPresent()) {
            throw new BadRequestException("Tax rate code already exists");
        }
        BigDecimal rate = scale(request.getRate());
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException("Tax rate must be between 0 and 1, for example 0.1500 for 15%");
        }

        ChartOfAccount outputAccount = request.getOutputAccountId() == null ? null : findAccountForTax(request.getOutputAccountId(), AccountType.LIABILITY, "Output tax");
        ChartOfAccount inputAccount = request.getInputAccountId() == null ? null : findAccountForTax(request.getInputAccountId(), AccountType.ASSET, "Input tax");

        TaxRate taxRate = new TaxRate();
        taxRate.setCode(code);
        taxRate.setName(request.getName().trim());
        taxRate.setRate(rate);
        taxRate.setOutputAccount(outputAccount);
        taxRate.setInputAccount(inputAccount);
        taxRate.setActive(request.getActive() == null || request.getActive());
        return mapTaxRate(taxRateRepository.save(taxRate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VatReturnRowDto> getVatReturn(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("VAT report from date cannot be after to date");
        }
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);

        return taxRateRepository.findAll().stream()
                .filter(TaxRate::isActive)
                .sorted(Comparator.comparing(TaxRate::getCode))
                .map(taxRate -> mapVatReturnRow(taxRate, fromDateTime, toDateTime))
                .toList();
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
    public List<JournalEntryAttachmentDto> getJournalEntryAttachments(UUID journalEntryId) {
        if (!journalEntryRepository.existsById(journalEntryId)) {
            throw new ResourceNotFoundException("JournalEntry", "id", journalEntryId);
        }
        return journalEntryAttachmentRepository.findByJournalEntryIdOrderByCreatedAtDesc(journalEntryId).stream()
                .map(this::mapJournalEntryAttachment)
                .toList();
    }

    @Override
    @Transactional
    public JournalEntryAttachmentDto uploadJournalEntryAttachment(UUID journalEntryId, MultipartFile file, String notes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required");
        }
        JournalEntry entry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", "id", journalEntryId));
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "attachment"));
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = "attachment";
        }

        String storagePath = fileStorageService.uploadFile(file, "journal-entries/" + journalEntryId);
        JournalEntryAttachment attachment = new JournalEntryAttachment();
        attachment.setJournalEntry(entry);
        attachment.setFilename(originalFilename);
        attachment.setContentType(blankToNull(file.getContentType()));
        attachment.setStoragePath(storagePath);
        attachment.setNotes(blankToNull(notes));
        return mapJournalEntryAttachment(journalEntryAttachmentRepository.save(attachment));
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryAttachmentDto getJournalEntryAttachment(UUID attachmentId) {
        return journalEntryAttachmentRepository.findById(attachmentId)
                .map(this::mapJournalEntryAttachment)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntryAttachment", "id", attachmentId));
    }

    @Override
    @Transactional
    public void deleteJournalEntryAttachment(UUID attachmentId) {
        JournalEntryAttachment attachment = journalEntryAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntryAttachment", "id", attachmentId));
        fileStorageService.deleteFile(attachment.getStoragePath());
        journalEntryAttachmentRepository.delete(attachment);
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
        PayableInvoiceSource source = resolvePayableInvoiceSource(request);

        BigDecimal totalAmount = scale(request.getTotalAmount() != null ? request.getTotalAmount() : source.defaultTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Accounts payable invoice amount must be greater than zero");
        }
        TaxRate taxRate = resolveTaxRate(request.getTaxRateId());
        TaxBreakdown taxBreakdown = calculateInclusiveTax(totalAmount, taxRate);
        if (taxBreakdown.taxAmount().compareTo(BigDecimal.ZERO) > 0 && taxRate.getInputAccount() == null) {
            throw new BadRequestException("Selected tax rate is missing an input tax account");
        }

        // Three-way match: PO ↔ GRN(s) ↔ Invoice. Skip when no PO is linked
        // (manual AP invoice). Caller can set force=true to override (logged in notes).
        String matchOverrideMemo = null;
        if (source.purchaseOrderId() != null) {
            matchOverrideMemo = sourceDocumentPort.assertThreeWayMatch(source.purchaseOrderId(), totalAmount, request.isForce());
        }

        LocalDate invoiceDate = request.getInvoiceDate() == null ? LocalDate.now() : request.getInvoiceDate();
        AccountsPayableInvoice invoice = new AccountsPayableInvoice();
        invoice.setInvoiceNumber(generateDocumentNumber("APV"));
        invoice.setSupplierInvoiceNumber(blankToNull(request.getSupplierInvoiceNumber()));
        invoice.setSourceSystem(sourceSystem(request.getSourceSystem(), source.sourceSystemFallback()));
        invoice.setSourceDocumentType(sourceDocumentType(request.getSourceDocumentType(), source.sourceDocumentTypeFallback()));
        invoice.setSourcePartyId(source.sourcePartyId());
        invoice.setSourcePartyName(source.sourcePartyName());
        invoice.setSourceDocumentId(source.sourceDocumentId());
        invoice.setSourceDocumentNumber(source.sourceDocumentNumber());
        invoice.setSupplierId(source.supplierId());
        invoice.setSupplierName(source.supplierName());
        invoice.setPurchaseOrderId(source.purchaseOrderId());
        invoice.setPurchaseOrderNumber(source.purchaseOrderNumber());
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.getDueDate() == null ? invoiceDate.plusDays(30) : request.getDueDate());
        invoice.setCurrency(defaultCurrency(request.getCurrency() != null ? request.getCurrency() : source.defaultCurrency()));
        invoice.setTotalAmount(totalAmount);
        invoice.setNetAmount(taxBreakdown.netAmount());
        invoice.setTaxAmount(taxBreakdown.taxAmount());
        invoice.setTaxRate(taxRate);
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        invoice.setBalanceDue(totalAmount);
        invoice.setStatus(InvoiceStatus.OPEN);
        String operatorNotes = blankToNull(request.getNotes());
        invoice.setNotes(matchOverrideMemo != null
                ? (operatorNotes != null ? operatorNotes + "\n" + matchOverrideMemo : matchOverrideMemo)
                : operatorNotes);
        invoice = accountsPayableInvoiceRepository.save(invoice);

        List<JournalLineSpec> postingLines = new ArrayList<>();
        postingLines.add(new JournalLineSpec("LIABILITY-GRN-ACCRUAL", "Goods receipt accrual clearing", taxBreakdown.netAmount(), BigDecimal.ZERO));
        if (taxBreakdown.taxAmount().compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount inputTaxAccount = taxRate.getInputAccount();
            postingLines.add(new JournalLineSpec(
                    inputTaxAccount.getAccountCode(),
                    inputTaxAccount.getAccountName(),
                    "Input tax " + taxRate.getCode(),
                    taxBreakdown.taxAmount(),
                    BigDecimal.ZERO
            ));
        }
        postingLines.add(new JournalLineSpec("LIABILITY-AP-TRADE", "Trade accounts payable", BigDecimal.ZERO, totalAmount));

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_PAYABLE_JOURNAL_CODE, "Accounts Payable Journal", "System journal for AP invoice and payment posting"),
                "AP_INVOICE",
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts payable invoice " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                postingLines
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
        Map<String, AgingSummaryRowDto> rows = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        accountsPayableInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(invoice -> referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()), String.CASE_INSENSITIVE_ORDER))
                .forEach(invoice -> applyAging(
                rows.computeIfAbsent(sourcePartyKey(invoice.getSourcePartyId(), invoice.getSupplierId()), ignored -> createAgingRow(invoice.getSupplierId(), referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()))),
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
        ReceivableInvoiceSource source = resolveReceivableInvoiceSource(request);

        BigDecimal totalAmount = scale(request.getTotalAmount() != null ? request.getTotalAmount() : source.defaultTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Accounts receivable invoice amount must be greater than zero");
        }
        TaxRate taxRate = resolveTaxRate(request.getTaxRateId());
        TaxBreakdown taxBreakdown = calculateInclusiveTax(totalAmount, taxRate);
        if (taxBreakdown.taxAmount().compareTo(BigDecimal.ZERO) > 0 && taxRate.getOutputAccount() == null) {
            throw new BadRequestException("Selected tax rate is missing an output tax account");
        }

        LocalDate invoiceDate = request.getInvoiceDate() == null ? LocalDate.now() : request.getInvoiceDate();
        AccountsReceivableInvoice invoice = new AccountsReceivableInvoice();
        invoice.setInvoiceNumber(generateDocumentNumber("ARV"));
        invoice.setCustomerInvoiceNumber(blankToNull(request.getCustomerInvoiceNumber()));
        invoice.setSourceSystem(sourceSystem(request.getSourceSystem(), source.sourceSystemFallback()));
        invoice.setSourceDocumentType(sourceDocumentType(request.getSourceDocumentType(), source.sourceDocumentTypeFallback()));
        invoice.setSourcePartyId(source.sourcePartyId());
        invoice.setSourcePartyName(source.sourcePartyName());
        invoice.setSourceDocumentId(source.sourceDocumentId());
        invoice.setSourceDocumentNumber(source.sourceDocumentNumber());
        invoice.setCustomerId(source.customerId());
        invoice.setCustomerName(source.customerName());
        invoice.setSalesOrderId(source.salesOrderId());
        invoice.setSalesOrderNumber(source.salesOrderNumber());
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.getDueDate() == null ? invoiceDate.plusDays(30) : request.getDueDate());
        invoice.setCurrency(defaultCurrency(request.getCurrency() != null ? request.getCurrency() : source.defaultCurrency()));
        invoice.setTotalAmount(totalAmount);
        invoice.setNetAmount(taxBreakdown.netAmount());
        invoice.setTaxAmount(taxBreakdown.taxAmount());
        invoice.setTaxRate(taxRate);
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        invoice.setBalanceDue(totalAmount);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setNotes(blankToNull(request.getNotes()));
        invoice = accountsReceivableInvoiceRepository.save(invoice);

        List<JournalLineSpec> postingLines = new ArrayList<>();
        postingLines.add(new JournalLineSpec("ASSET-AR-TRADE", "Trade accounts receivable", totalAmount, BigDecimal.ZERO));
        postingLines.add(new JournalLineSpec("REVENUE-SALES", "Sales revenue", BigDecimal.ZERO, taxBreakdown.netAmount()));
        if (taxBreakdown.taxAmount().compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount outputTaxAccount = taxRate.getOutputAccount();
            postingLines.add(new JournalLineSpec(
                    outputTaxAccount.getAccountCode(),
                    outputTaxAccount.getAccountName(),
                    "Output tax " + taxRate.getCode(),
                    BigDecimal.ZERO,
                    taxBreakdown.taxAmount()
            ));
        }

        createPostedJournalEntry(
                ensureSystemJournal(ACCOUNTS_RECEIVABLE_JOURNAL_CODE, "Accounts Receivable Journal", "System journal for AR invoice and receipt posting"),
                "AR_INVOICE",
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                "Accounts receivable invoice " + invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                postingLines
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
        Map<String, AgingSummaryRowDto> rows = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        accountsReceivableInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(invoice -> referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()), String.CASE_INSENSITIVE_ORDER))
                .forEach(invoice -> applyAging(
                rows.computeIfAbsent(sourcePartyKey(invoice.getSourcePartyId(), invoice.getCustomerId()), ignored -> createAgingRow(invoice.getCustomerId(), referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()))),
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
        accountingPeriodService.assertOpen(entry.getEntryDate());
        entry.setPostedAt(LocalDateTime.now());
        return mapEntry(journalEntryRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringJournalTemplateDto> getRecurringJournalTemplates() {
        return recurringJournalTemplateRepository.findAll().stream()
                .sorted(Comparator.comparing(RecurringJournalTemplate::getNextRunDate)
                        .thenComparing(RecurringJournalTemplate::getTemplateCode))
                .map(this::mapRecurringTemplate)
                .toList();
    }

    @Override
    @Transactional
    public RecurringJournalTemplateDto createRecurringJournalTemplate(CreateRecurringJournalTemplateRequest request) {
        if (!StringUtils.hasText(request.getTemplateCode())
                || !StringUtils.hasText(request.getTemplateName())
                || request.getJournalId() == null
                || request.getCadence() == null
                || request.getNextRunDate() == null
                || request.getLines() == null
                || request.getLines().isEmpty()) {
            throw new BadRequestException("Template code, name, journal, cadence, next run date, and lines are required");
        }

        String templateCode = request.getTemplateCode().trim().toUpperCase(Locale.ROOT);
        if (recurringJournalTemplateRepository.findByTemplateCode(templateCode).isPresent()) {
            throw new BadRequestException("Recurring journal template code already exists");
        }

        AccountingJournal journal = accountingJournalRepository.findById(request.getJournalId())
                .orElseThrow(() -> new ResourceNotFoundException("AccountingJournal", "id", request.getJournalId()));
        if (!journal.isActive()) {
            throw new BadRequestException("Selected journal is inactive");
        }

        RecurringJournalTemplate template = new RecurringJournalTemplate();
        template.setTemplateCode(templateCode);
        template.setTemplateName(request.getTemplateName().trim());
        template.setJournal(journal);
        template.setMemo(blankToNull(request.getMemo()));
        template.setCurrency(defaultCurrency(request.getCurrency()));
        template.setCadence(request.getCadence());
        template.setNextRunDate(request.getNextRunDate());
        template.setActive(request.getActive() == null || request.getActive());

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int lineNumber = 1;
        for (CreateRecurringJournalTemplateRequest.LineRequest lineRequest : request.getLines()) {
            ChartOfAccount account = chartOfAccountRepository.findById(lineRequest.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", lineRequest.getAccountId()));
            if (!account.isActive()) {
                throw new BadRequestException("Template line account is inactive: " + account.getAccountCode());
            }
            if (!account.isAllowManualPosting()) {
                throw new BadRequestException("Recurring journals can only post to manual-posting accounts: " + account.getAccountCode());
            }

            BigDecimal debit = scale(lineRequest.getDebitAmount());
            BigDecimal credit = scale(lineRequest.getCreditAmount());
            if ((debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0)
                    || (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0)) {
                throw new BadRequestException("Each recurring journal line must have either a debit or a credit amount");
            }

            RecurringJournalTemplateLine line = new RecurringJournalTemplateLine();
            line.setTemplate(template);
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(blankToNull(lineRequest.getDescription()));
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            template.getLines().add(line);
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Recurring journal template must be balanced");
        }

        return mapRecurringTemplate(recurringJournalTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public List<JournalEntryDto> runDueRecurringJournalTemplates(LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now() : runDate;
        List<RecurringJournalTemplate> dueTemplates = recurringJournalTemplateRepository
                .findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(effectiveRunDate);
        return runRecurringTemplates(dueTemplates, effectiveRunDate);
    }

    @Transactional
    public List<JournalEntryDto> runDueRecurringJournalTemplatesForTenant(String tenantId, LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now() : runDate;
        List<RecurringJournalTemplate> dueTemplates = recurringJournalTemplateRepository
                .findByTenantIdAndActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(tenantId, effectiveRunDate);
        return runRecurringTemplates(dueTemplates, effectiveRunDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialEventDto> getPendingFinancialEvents() {
        return financialEventRepository.findAll().stream()
                .filter(event -> event.getPostingStatus() == PostingStatus.PENDING || event.getPostingStatus() == PostingStatus.FAILED)
                .sorted(Comparator.comparing(FinancialEvent::getOccurredAt).reversed())
                .map(this::mapFinancialEvent)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialEventDto getFinancialEventPreview(UUID financialEventId) {
        return financialEventRepository.findById(financialEventId)
                .map(this::mapFinancialEvent)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialEvent", "id", financialEventId));
    }

    @Override
    @Transactional
    public List<JournalEntryDto> postFinancialEvents(List<UUID> financialEventIds) {
        if (financialEventIds == null || financialEventIds.isEmpty()) {
            throw new BadRequestException("At least one financial event is required");
        }
        List<JournalEntryDto> results = new ArrayList<>();
        for (UUID financialEventId : financialEventIds) {
            try {
                results.add(postFinancialEvent(financialEventId));
            } catch (RuntimeException ex) {
                financialEventFailureService.markFailed(financialEventId, ex.getMessage());
            }
        }
        return results;
    }

    @Override
    @Transactional
    public JournalEntryDto postFinancialEvent(UUID financialEventId) {
        FinancialEvent event = financialEventRepository.findById(financialEventId)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialEvent", "id", financialEventId));

        if (event.getPostingStatus() != PostingStatus.PENDING && event.getPostingStatus() != PostingStatus.FAILED) {
            throw new BadRequestException("Only pending or failed financial events can be posted");
        }
        event.setPostingStatus(PostingStatus.PENDING);
        event.setFailureReason(null);
        event.getSubledgerEntries().forEach(line -> line.setPostingStatus(PostingStatus.PENDING));

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
            try {
                results.add(postFinancialEvent(event.getId()));
            } catch (RuntimeException ex) {
                financialEventFailureService.markFailed(event.getId(), ex.getMessage());
            }
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
        accountingPeriodService.assertOpen(reversal.getEntryDate());
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

    @Override
    @Transactional(readOnly = true)
    public List<CashFlowRowDto> getCashFlow(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Cash flow from date cannot be after to date");
        }
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);

        Map<String, BigDecimal> amountsByKey = new LinkedHashMap<>();
        for (JournalEntry entry : journalEntryRepository.findPostedEntriesWithLines(fromDateTime, toDateTime)) {
            BigDecimal cashImpact = entry.getLines().stream()
                    .filter(line -> isCashEquivalentAccount(line.getAccount()))
                    .map(line -> scale(line.getDebitAmount()).subtract(scale(line.getCreditAmount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            cashImpact = scale(cashImpact);
            if (cashImpact.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            String section = classifyCashFlowSection(entry);
            String label = cashFlowLabel(entry);
            String key = section + "\n" + label;
            amountsByKey.merge(key, cashImpact, BigDecimal::add);
        }

        List<CashFlowRowDto> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountsByKey.entrySet()) {
            String[] key = entry.getKey().split("\n", 2);
            CashFlowRowDto row = new CashFlowRowDto();
            row.setSection(key[0]);
            row.setLabel(key.length > 1 ? key[1] : key[0]);
            row.setAmount(scale(entry.getValue()));
            rows.add(row);
        }
        return rows;
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
        return createPostedJournalEntry(journal, sourceDocumentType, sourceDocumentId, sourceDocumentNumber, memo, currency, LocalDateTime.now(), lines);
    }

    private JournalEntry createPostedJournalEntry(AccountingJournal journal,
                                                  String sourceDocumentType,
                                                  String sourceDocumentId,
                                                  String sourceDocumentNumber,
                                                  String memo,
                                                  String currency,
                                                  LocalDateTime entryDate,
                                                  List<JournalLineSpec> lines) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber());
        entry.setJournal(journal);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setEntryDate(entryDate == null ? LocalDateTime.now() : entryDate);
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
            line.setDescription(blankToNull(spec.description()) == null ? spec.accountName() : spec.description());
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
        accountingPeriodService.assertOpen(entry.getEntryDate());
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
            sourceDocumentPort.findCashReconciliationSeeds(businessDate).stream()
                .map(seed -> new ReconciliationLineSeed(
                    seed.sourceType(),
                    seed.sourceId(),
                    seed.sourceReference(),
                    seed.transactionDate(),
                    seed.description(),
                    scale(seed.amount())
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
                        "AR receipt from " + referenceName(payment.getInvoice().getSourcePartyName(), payment.getInvoice().getSourcePartyId()),
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
                        "AP payment to " + referenceName(payment.getInvoice().getSourcePartyName(), payment.getInvoice().getSourcePartyId()),
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

    private TaxRate resolveTaxRate(UUID taxRateId) {
        if (taxRateId == null) {
            return null;
        }
        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxRate", "id", taxRateId));
        if (!taxRate.isActive()) {
            throw new BadRequestException("Selected tax rate is inactive");
        }
        return taxRate;
    }

    private TaxBreakdown calculateInclusiveTax(BigDecimal grossAmount, TaxRate taxRate) {
        BigDecimal gross = scale(grossAmount);
        if (taxRate == null || taxRate.getRate() == null || taxRate.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            return new TaxBreakdown(gross, BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        }
        BigDecimal divisor = BigDecimal.ONE.add(taxRate.getRate());
        BigDecimal net = gross.divide(divisor, 6, RoundingMode.HALF_UP);
        return new TaxBreakdown(net, scale(gross.subtract(net)));
    }

    private BigDecimal accountBalance(ChartOfAccount account, BigDecimal debits, BigDecimal credits) {
        BigDecimal debitAmount = scale(debits);
        BigDecimal creditAmount = scale(credits);
        if (account.getAccountType() == AccountType.LIABILITY
                || account.getAccountType() == AccountType.EQUITY
                || account.getAccountType() == AccountType.REVENUE) {
            return scale(creditAmount.subtract(debitAmount));
        }
        return scale(debitAmount.subtract(creditAmount));
    }

    private BigDecimal lineImpact(ChartOfAccount account, JournalEntryLine line) {
        return accountBalance(account, line.getDebitAmount(), line.getCreditAmount());
    }

    private List<JournalEntryDto> runRecurringTemplates(List<RecurringJournalTemplate> dueTemplates, LocalDate runDate) {
        List<JournalEntryDto> entries = new ArrayList<>();
        for (RecurringJournalTemplate template : dueTemplates) {
            entries.add(createRecurringJournalEntry(template, runDate));
            template.setLastRunAt(LocalDateTime.now());
            template.setNextRunDate(nextRunDate(template.getNextRunDate(), template.getCadence()));
            recurringJournalTemplateRepository.save(template);
        }
        return entries;
    }

    private JournalEntryDto createRecurringJournalEntry(RecurringJournalTemplate template, LocalDate runDate) {
        List<JournalLineSpec> lines = template.getLines().stream()
                .sorted(Comparator.comparing(RecurringJournalTemplateLine::getLineNumber))
                .map(line -> new JournalLineSpec(
                        line.getAccount().getAccountCode(),
                        line.getAccount().getAccountName(),
                        line.getDescription(),
                        line.getDebitAmount(),
                        line.getCreditAmount()))
                .toList();
        JournalEntry entry = createPostedJournalEntry(
                template.getJournal(),
                "RECURRING_JOURNAL",
                template.getId() + ":" + runDate,
                template.getTemplateCode(),
                blankToNull(template.getMemo()) == null ? "Recurring journal " + template.getTemplateName() : template.getMemo(),
                template.getCurrency(),
                runDate.atStartOfDay(),
                lines
        );
        return mapEntry(entry);
    }

    private LocalDate nextRunDate(LocalDate current, RecurringJournalCadence cadence) {
        return switch (cadence) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    private boolean isCashEquivalentAccount(ChartOfAccount account) {
        if (account == null || account.getAccountType() != AccountType.ASSET) {
            return false;
        }
        String code = account.getAccountCode() == null ? "" : account.getAccountCode().toUpperCase(Locale.ROOT);
        String name = account.getAccountName() == null ? "" : account.getAccountName().toUpperCase(Locale.ROOT);
        String combined = code + " " + name;
        return combined.contains("CASH")
                || combined.contains("BANK")
                || combined.contains("WALLET")
                || combined.contains("CLEARING")
                || combined.contains("DEPOSIT");
    }

    private String classifyCashFlowSection(JournalEntry entry) {
        boolean hasRevenueOrExpense = entry.getLines().stream()
                .filter(line -> !isCashEquivalentAccount(line.getAccount()))
                .map(line -> line.getAccount().getAccountType())
                .anyMatch(type -> type == AccountType.REVENUE || type == AccountType.EXPENSE);
        if (hasRevenueOrExpense) {
            return "Operating Activities";
        }

        boolean hasFinancingAccount = entry.getLines().stream()
                .filter(line -> !isCashEquivalentAccount(line.getAccount()))
                .map(line -> line.getAccount().getAccountType())
                .anyMatch(type -> type == AccountType.LIABILITY || type == AccountType.EQUITY);
        if (hasFinancingAccount) {
            return "Financing Activities";
        }

        return "Investing Activities";
    }

    private String cashFlowLabel(JournalEntry entry) {
        if (StringUtils.hasText(entry.getSourceDocumentType())) {
            return entry.getSourceDocumentType().replace('_', ' ');
        }
        return entry.getJournal().getJournalName();
    }

    private String defaultCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PayableInvoiceSource resolvePayableInvoiceSource(CreateAccountsPayableInvoiceRequest request) {
        if (request.getSupplierId() != null || request.getPurchaseOrderId() != null) {
            var source = sourceDocumentPort.resolvePayableSource(request.getSupplierId(), request.getPurchaseOrderId());
            String partyId = source.supplierId() == null ? null : source.supplierId().toString();
            String documentId = source.purchaseOrderId() == null ? partyId : source.purchaseOrderId().toString();
            String documentNumber = StringUtils.hasText(source.purchaseOrderNumber())
                    ? source.purchaseOrderNumber()
                    : firstText(request.getSupplierInvoiceNumber(), source.supplierName());
            return new PayableInvoiceSource(
                    source.supplierId(),
                    source.supplierName(),
                    source.purchaseOrderId(),
                    source.purchaseOrderNumber(),
                    partyId,
                    source.supplierName(),
                    documentId,
                    documentNumber,
                    source.defaultTotalAmount(),
                    source.defaultCurrency(),
                    "INVENTORY",
                    source.purchaseOrderId() == null ? "SUPPLIER" : "PURCHASE_ORDER"
            );
        }

        String partyId = blankToNull(request.getSourcePartyId());
        String partyName = blankToNull(request.getSourcePartyName());
        if (!StringUtils.hasText(partyId) && !StringUtils.hasText(partyName)) {
            throw new BadRequestException("Supplier, purchase order, or source party is required");
        }
        String documentId = firstText(request.getSourceDocumentId(), partyId);
        String documentNumber = firstText(request.getSourceDocumentNumber(), request.getSupplierInvoiceNumber(), partyName, documentId);
        return new PayableInvoiceSource(
                null,
                partyName,
                null,
                null,
                partyId,
                partyName,
                documentId,
                documentNumber,
                null,
                null,
                "EXTERNAL",
                StringUtils.hasText(documentId) ? "EXTERNAL_DOCUMENT" : "EXTERNAL_PARTY"
        );
    }

    private ReceivableInvoiceSource resolveReceivableInvoiceSource(CreateAccountsReceivableInvoiceRequest request) {
        if (request.getCustomerId() != null || request.getSalesOrderId() != null) {
            var source = sourceDocumentPort.resolveReceivableSource(request.getCustomerId(), request.getSalesOrderId());
            String partyId = source.customerId() == null ? null : source.customerId().toString();
            String documentId = source.salesOrderId() == null ? partyId : source.salesOrderId().toString();
            String documentNumber = StringUtils.hasText(source.salesOrderNumber())
                    ? source.salesOrderNumber()
                    : firstText(request.getCustomerInvoiceNumber(), source.customerName());
            return new ReceivableInvoiceSource(
                    source.customerId(),
                    source.customerName(),
                    source.salesOrderId(),
                    source.salesOrderNumber(),
                    partyId,
                    source.customerName(),
                    documentId,
                    documentNumber,
                    source.defaultTotalAmount(),
                    source.defaultCurrency(),
                    "INVENTORY",
                    source.salesOrderId() == null ? "CUSTOMER" : "SALES_ORDER"
            );
        }

        String partyId = blankToNull(request.getSourcePartyId());
        String partyName = blankToNull(request.getSourcePartyName());
        if (!StringUtils.hasText(partyId) && !StringUtils.hasText(partyName)) {
            throw new BadRequestException("Customer, sales order, or source party is required");
        }
        String documentId = firstText(request.getSourceDocumentId(), partyId);
        String documentNumber = firstText(request.getSourceDocumentNumber(), request.getCustomerInvoiceNumber(), partyName, documentId);
        return new ReceivableInvoiceSource(
                null,
                partyName,
                null,
                null,
                partyId,
                partyName,
                documentId,
                documentNumber,
                null,
                null,
                "EXTERNAL",
                StringUtils.hasText(documentId) ? "EXTERNAL_DOCUMENT" : "EXTERNAL_PARTY"
        );
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String referenceName(String name, UUID id) {
        return referenceName(name, id == null ? null : id.toString());
    }

    private String referenceName(String name, String id) {
        return StringUtils.hasText(name) ? name : StringUtils.hasText(id) ? id : "Unknown";
    }

    private String sourcePartyKey(String sourcePartyId, UUID legacyPartyId) {
        if (StringUtils.hasText(sourcePartyId)) {
            return sourcePartyId;
        }
        return legacyPartyId == null ? "UNKNOWN" : legacyPartyId.toString();
    }

    private String sourceSystem(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String sourceDocumentType(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

        private record PayableInvoiceSource(
            UUID supplierId,
            String supplierName,
            UUID purchaseOrderId,
            String purchaseOrderNumber,
            String sourcePartyId,
            String sourcePartyName,
            String sourceDocumentId,
            String sourceDocumentNumber,
            BigDecimal defaultTotalAmount,
            String defaultCurrency,
            String sourceSystemFallback,
            String sourceDocumentTypeFallback) {
        }

        private record ReceivableInvoiceSource(
            UUID customerId,
            String customerName,
            UUID salesOrderId,
            String salesOrderNumber,
            String sourcePartyId,
            String sourcePartyName,
            String sourceDocumentId,
            String sourceDocumentNumber,
            BigDecimal defaultTotalAmount,
            String defaultCurrency,
            String sourceSystemFallback,
            String sourceDocumentTypeFallback) {
        }

    private ChartOfAccount findAccountForTax(UUID accountId, AccountType expectedType, String label) {
        ChartOfAccount account = chartOfAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", accountId));
        if (account.getAccountType() != expectedType) {
            throw new BadRequestException(label + " account must be an " + expectedType + " account");
        }
        if (!account.isActive()) {
            throw new BadRequestException(label + " account must be active");
        }
        return account;
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

    private TaxRateDto mapTaxRate(TaxRate taxRate) {
        TaxRateDto dto = new TaxRateDto();
        dto.setId(taxRate.getId());
        dto.setCode(taxRate.getCode());
        dto.setName(taxRate.getName());
        dto.setRate(taxRate.getRate());
        dto.setActive(taxRate.isActive());
        if (taxRate.getOutputAccount() != null) {
            dto.setOutputAccountId(taxRate.getOutputAccount().getId());
            dto.setOutputAccountName(taxRate.getOutputAccount().getAccountCode() + " · " + taxRate.getOutputAccount().getAccountName());
        }
        if (taxRate.getInputAccount() != null) {
            dto.setInputAccountId(taxRate.getInputAccount().getId());
            dto.setInputAccountName(taxRate.getInputAccount().getAccountCode() + " · " + taxRate.getInputAccount().getAccountName());
        }
        return dto;
    }

    private RecurringJournalTemplateDto mapRecurringTemplate(RecurringJournalTemplate template) {
        RecurringJournalTemplateDto dto = new RecurringJournalTemplateDto();
        dto.setId(template.getId());
        dto.setTemplateCode(template.getTemplateCode());
        dto.setTemplateName(template.getTemplateName());
        dto.setJournalId(template.getJournal().getId());
        dto.setJournalCode(template.getJournal().getJournalCode());
        dto.setJournalName(template.getJournal().getJournalName());
        dto.setMemo(template.getMemo());
        dto.setCurrency(template.getCurrency());
        dto.setCadence(template.getCadence());
        dto.setNextRunDate(template.getNextRunDate());
        dto.setLastRunAt(template.getLastRunAt());
        dto.setActive(template.isActive());
        dto.setLines(template.getLines().stream()
                .sorted(Comparator.comparing(RecurringJournalTemplateLine::getLineNumber))
                .map(this::mapRecurringTemplateLine)
                .toList());
        return dto;
    }

    private RecurringJournalTemplateDto.LineDto mapRecurringTemplateLine(RecurringJournalTemplateLine line) {
        RecurringJournalTemplateDto.LineDto dto = new RecurringJournalTemplateDto.LineDto();
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

    private VatReturnRowDto mapVatReturnRow(TaxRate taxRate, LocalDateTime from, LocalDateTime to) {
        BigDecimal outputTax = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        if (taxRate.getOutputAccount() != null) {
            BigDecimal outputCredits = journalEntryLineRepository.sumCreditsByAccountBetween(taxRate.getOutputAccount().getId(), from, to);
            BigDecimal outputDebits = journalEntryLineRepository.sumDebitsByAccountBetween(taxRate.getOutputAccount().getId(), from, to);
            outputTax = scale(outputCredits.subtract(outputDebits).max(BigDecimal.ZERO));
        }

        BigDecimal inputTax = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        if (taxRate.getInputAccount() != null) {
            BigDecimal inputDebits = journalEntryLineRepository.sumDebitsByAccountBetween(taxRate.getInputAccount().getId(), from, to);
            BigDecimal inputCredits = journalEntryLineRepository.sumCreditsByAccountBetween(taxRate.getInputAccount().getId(), from, to);
            inputTax = scale(inputDebits.subtract(inputCredits).max(BigDecimal.ZERO));
        }

        VatReturnRowDto dto = new VatReturnRowDto();
        dto.setTaxRateId(taxRate.getId());
        dto.setCode(taxRate.getCode());
        dto.setName(taxRate.getName());
        dto.setRate(taxRate.getRate());
        dto.setOutputTax(outputTax);
        dto.setInputTax(inputTax);
        dto.setNetTaxPayable(scale(outputTax.subtract(inputTax)));
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

    private AccountingAuditLogDto mapAuditLog(AccountingAuditLog auditLog) {
        AccountingAuditLogDto dto = new AccountingAuditLogDto();
        dto.setId(auditLog.getId());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setAction(auditLog.getAction());
        dto.setBeforeState(auditLog.getBeforeState());
        dto.setAfterState(auditLog.getAfterState());
        dto.setUserId(auditLog.getUserId());
        dto.setOccurredAt(auditLog.getOccurredAt());
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

    private FinancialEventDto mapFinancialEvent(FinancialEvent event) {
        FinancialEventDto dto = new FinancialEventDto();
        dto.setId(event.getId());
        dto.setEventNumber(event.getEventNumber());
        dto.setEventType(event.getEventType());
        dto.setSourceDocumentType(event.getSourceDocumentType());
        dto.setSourceDocumentId(event.getSourceDocumentId());
        dto.setSourceDocumentNumber(event.getSourceDocumentNumber());
        dto.setExternalReference(event.getExternalReference());
        dto.setSummary(event.getSummary());
        dto.setTotalAmount(event.getTotalAmount());
        dto.setCurrency(event.getCurrency());
        dto.setPostingStatus(event.getPostingStatus());
        dto.setFailureReason(event.getFailureReason());
        dto.setOccurredAt(event.getOccurredAt());
        dto.setActorName(event.getActorName());
        dto.setMetadataJson(event.getMetadataJson());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setSubledgerEntries(event.getSubledgerEntries().stream()
                .sorted(Comparator.comparing(SubledgerEntry::getLineNumber))
                .map(this::mapSubledgerEntry)
                .toList());
        return dto;
    }

    private SubledgerEntryDto mapSubledgerEntry(SubledgerEntry entry) {
        SubledgerEntryDto dto = new SubledgerEntryDto();
        dto.setId(entry.getId());
        dto.setLineNumber(entry.getLineNumber());
        dto.setEntryType(entry.getEntryType());
        dto.setAccountCode(entry.getAccountCode());
        dto.setAccountName(entry.getAccountName());
        dto.setDescription(entry.getDescription());
        dto.setAmount(entry.getAmount());
        dto.setCurrency(entry.getCurrency());
        dto.setSourceDocumentType(entry.getSourceDocumentType());
        dto.setSourceDocumentId(entry.getSourceDocumentId());
        dto.setSourceDocumentNumber(entry.getSourceDocumentNumber());
        dto.setPostingStatus(entry.getPostingStatus());
        return dto;
    }

    private JournalEntryAttachmentDto mapJournalEntryAttachment(JournalEntryAttachment attachment) {
        JournalEntryAttachmentDto dto = new JournalEntryAttachmentDto();
        dto.setId(attachment.getId());
        dto.setJournalEntryId(attachment.getJournalEntry().getId());
        dto.setEntryNumber(attachment.getJournalEntry().getEntryNumber());
        dto.setFilename(attachment.getFilename());
        dto.setContentType(attachment.getContentType());
        dto.setStoragePath(attachment.getStoragePath());
        dto.setNotes(attachment.getNotes());
        dto.setCreatedAt(attachment.getCreatedAt());
        dto.setUpdatedAt(attachment.getUpdatedAt());
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
        dto.setSourceSystem(invoice.getSourceSystem());
        dto.setSourceDocumentType(invoice.getSourceDocumentType());
        dto.setSourcePartyId(invoice.getSourcePartyId());
        dto.setSourcePartyName(referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()));
        dto.setSourceDocumentId(invoice.getSourceDocumentId());
        dto.setSourceDocumentNumber(invoice.getSourceDocumentNumber());
        dto.setSupplierId(invoice.getSupplierId());
        dto.setSupplierName(referenceName(invoice.getSupplierName(), invoice.getSupplierId()));
        dto.setPurchaseOrderId(invoice.getPurchaseOrderId());
        dto.setPurchaseOrderNumber(invoice.getPurchaseOrderNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setCurrency(invoice.getCurrency());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setTaxAmount(invoice.getTaxAmount());
        if (invoice.getTaxRate() != null) {
            dto.setTaxRateId(invoice.getTaxRate().getId());
            dto.setTaxRateCode(invoice.getTaxRate().getCode());
            dto.setTaxRateName(invoice.getTaxRate().getName());
        }
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
        dto.setSourceSystem(invoice.getSourceSystem());
        dto.setSourceDocumentType(invoice.getSourceDocumentType());
        dto.setSourcePartyId(invoice.getSourcePartyId());
        dto.setSourcePartyName(referenceName(invoice.getSourcePartyName(), invoice.getSourcePartyId()));
        dto.setSourceDocumentId(invoice.getSourceDocumentId());
        dto.setSourceDocumentNumber(invoice.getSourceDocumentNumber());
        dto.setCustomerId(invoice.getCustomerId());
        dto.setCustomerName(referenceName(invoice.getCustomerName(), invoice.getCustomerId()));
        dto.setSalesOrderId(invoice.getSalesOrderId());
        dto.setSalesOrderNumber(invoice.getSalesOrderNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setCurrency(invoice.getCurrency());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setTaxAmount(invoice.getTaxAmount());
        if (invoice.getTaxRate() != null) {
            dto.setTaxRateId(invoice.getTaxRate().getId());
            dto.setTaxRateCode(invoice.getTaxRate().getCode());
            dto.setTaxRateName(invoice.getTaxRate().getName());
        }
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

    private record JournalLineSpec(String accountCode, String accountName, String description, BigDecimal debitAmount, BigDecimal creditAmount) {
        private JournalLineSpec(String accountCode, String accountName, BigDecimal debitAmount, BigDecimal creditAmount) {
            this(accountCode, accountName, accountName, debitAmount, creditAmount);
        }
    }

    private record TaxBreakdown(BigDecimal netAmount, BigDecimal taxAmount) {
    }

    private record ReconciliationLineSeed(ReconciliationSourceType sourceType,
                                          String sourceId,
                                          String sourceReference,
                                          LocalDate transactionDate,
                                          String description,
                                          BigDecimal amount) {
    }
}
